package com.kms3735.threelinesfirst.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kms3735.threelinesfirst.BuildConfig;
import com.kms3735.threelinesfirst.R;
import com.kms3735.threelinesfirst.ai.Summary;
import com.kms3735.threelinesfirst.ai.SummaryClient;
import com.kms3735.threelinesfirst.data.AppDatabase;
import com.kms3735.threelinesfirst.data.Notice;
import com.kms3735.threelinesfirst.data.NoticeDao;
import com.kms3735.threelinesfirst.data.Todo;
import com.kms3735.threelinesfirst.verify.Verifier;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ③ 세 줄 요약: 개인정보를 가린 글을 AI로 요약해 세 줄, 경고, 할 일 체크박스로 보여 준다.
 * 요약 속 날짜·금액은 원문과 대조해, 다르면 한 번 다시 요약하고 그래도 다르면 경고 배지를 붙인다.
 * 안내문은 Room에 저장돼 있고, 이미 요약한 안내문은 AI를 다시 부르지 않고 저장된 요약을 보여 준다.
 */
public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    public static final String EXTRA_NOTICE_ID = "notice_id";

    /** 무료 사용량을 지키려고 하루 요청 횟수를 제한한다 */
    private static final int DAILY_LIMIT = 30;

    private TextView txtStatus, txtVerify, txtLine1, txtLine2, txtLine3, txtWarning, txtTodosTitle;
    private Button btnRetry, btnPhoto;
    private ImageView imgOriginal;
    private View summaryGroup;
    private LinearLayout todoGroup;

    private NoticeDao dao;
    private Notice notice;
    private String maskedText;
    /** 원문 사진 경로. 공유받은 글처럼 사진이 없으면 null */
    private String photoPath;
    private SummaryClient client;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    /** 대조에 실패해 이미 한 번 다시 요약했는지 */
    private boolean resummarized;

    private final SummaryClient.Callback callback = new SummaryClient.Callback() {
        @Override
        public void onSuccess(Summary summary) {
            if (!isDestroyed()) onSummary(summary);
        }

        @Override
        public void onFailure() {
            if (!isDestroyed()) showStatus(R.string.summary_error, true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtStatus = findViewById(R.id.txtStatus);
        btnRetry = findViewById(R.id.btnRetry);
        summaryGroup = findViewById(R.id.summaryGroup);
        txtVerify = findViewById(R.id.txtVerify);
        txtLine1 = findViewById(R.id.txtLine1);
        txtLine2 = findViewById(R.id.txtLine2);
        txtLine3 = findViewById(R.id.txtLine3);
        btnPhoto = findViewById(R.id.btnPhoto);
        imgOriginal = findViewById(R.id.imgOriginal);
        txtWarning = findViewById(R.id.txtWarning);
        txtTodosTitle = findViewById(R.id.txtTodosTitle);
        todoGroup = findViewById(R.id.todoGroup);

        btnPhoto.setOnClickListener(v -> {
            if (imgOriginal.getVisibility() == View.VISIBLE) hidePhoto();
            else showPhoto();
        });

        btnRetry.setOnClickListener(v -> summarize());
        findViewById(R.id.btnHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());

        client = new SummaryClient(BuildConfig.GEMINI_API_KEY);
        dao = AppDatabase.get(this).noticeDao();
        long id = getIntent().getLongExtra(EXTRA_NOTICE_ID, -1);
        new Thread(() -> {
            Notice n = dao.get(id);
            List<Todo> todos = n == null ? null : dao.getTodos(id);
            uiHandler.post(() -> {
                if (isDestroyed()) return;
                if (n == null) {
                    finish();
                    return;
                }
                onLoaded(n, todos);
            });
        }).start();
    }

    private void onLoaded(Notice n, List<Todo> todos) {
        notice = n;
        maskedText = n.maskedText == null ? "" : n.maskedText;
        ((TextView) findViewById(R.id.txtOriginal)).setText(maskedText);
        photoPath = n.imagePath;
        if (photoPath != null && !new File(photoPath).exists()) photoPath = null;
        btnPhoto.setVisibility(photoPath != null ? View.VISIBLE : View.GONE);

        if (n.isSummarized()) showSummary(todos);
        else summarize();
    }

    private void summarize() {
        if (BuildConfig.GEMINI_API_KEY.isEmpty()) {
            showStatus(R.string.summary_no_key, false);
            return;
        }
        if (!useDailyQuota()) {
            showStatus(R.string.summary_limit, false);
            return;
        }
        showStatus(R.string.summarizing, false);
        resummarized = false;
        client.summarize(maskedText, LocalDate.now(), callback);
    }

    /** 날짜·금액을 원문과 대조한다. 다르면 한 번만 다시 요약한다 (하루 횟수에는 세지 않음) */
    private void onSummary(Summary summary) {
        Verifier.Result result = Verifier.check(maskedText, summary, LocalDate.now());
        if (!result.passed() && !resummarized) {
            resummarized = true;
            Log.d(TAG, "원문과 다름, 다시 요약: " + result.mismatches);
            String note = getString(R.string.verify_note, String.join(", ", result.mismatches));
            client.summarize(maskedText, LocalDate.now(), note, callback);
            return;
        }
        notice.apply(summary, result);
        List<Todo> todos = new ArrayList<>();
        for (String content : summary.todos) {
            Todo t = new Todo();
            t.noticeId = notice.id;
            t.content = content;
            todos.add(t);
        }
        // 저장해야 할 일의 id가 생기므로, 저장한 뒤 다시 읽어 보여 준다
        new Thread(() -> {
            dao.saveSummary(notice, todos);
            List<Todo> saved = dao.getTodos(notice.id);
            uiHandler.post(() -> {
                if (!isDestroyed()) showSummary(saved);
            });
        }).start();
    }

    private void showStatus(int messageRes, boolean canRetry) {
        txtStatus.setText(messageRes);
        txtStatus.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
        summaryGroup.setVisibility(View.GONE);
    }

    private void showSummary(List<Todo> todos) {
        Notice n = notice;
        txtStatus.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        summaryGroup.setVisibility(View.VISIBLE);

        showVerifyBadge(n.checkedCount, n.mismatches);
        txtLine1.setText(n.line1What);
        txtLine2.setText(n.line2Todo);
        txtLine3.setText(n.line3WhenWhere);
        String warning = n.warning == null ? "" : n.warning;
        txtWarning.setText(warning);
        txtWarning.setVisibility(warning.trim().isEmpty() ? View.GONE : View.VISIBLE);

        // 체크하면 바로 저장해서, 앱을 다시 켜도 체크 상태가 남는다
        todoGroup.removeAllViews();
        float textPx = getResources().getDimension(R.dimen.text_body);
        int minHeight = getResources().getDimensionPixelSize(R.dimen.touch_min);
        for (Todo todo : todos) {
            CheckBox box = new CheckBox(this);
            box.setText(todo.content);
            box.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
            box.setMinHeight(minHeight);
            box.setChecked(todo.done);
            box.setOnCheckedChangeListener((b, checked) -> {
                todo.done = checked;
                new Thread(() -> dao.updateTodo(todo)).start();
            });
            todoGroup.addView(box);
        }
        txtTodosTitle.setVisibility(todos.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** 통과면 초록 체크, 다르면 주황 경고와 함께 원문 사진을 펼친다. 대조할 값이 없으면 숨긴다 */
    private void showVerifyBadge(int checked, String mismatches) {
        if (checked == 0) {
            txtVerify.setVisibility(View.GONE);
            return;
        }
        txtVerify.setVisibility(View.VISIBLE);
        if (mismatches == null || mismatches.isEmpty()) {
            txtVerify.setText(R.string.verified);
            txtVerify.setBackgroundResource(R.drawable.verified_box);
            txtVerify.setTextColor(ContextCompat.getColor(this, R.color.verified_text));
            txtVerify.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
        } else {
            txtVerify.setText(getString(photoPath != null ? R.string.verify_failed : R.string.verify_failed_no_photo, mismatches));
            txtVerify.setBackgroundResource(R.drawable.warning_box);
            txtVerify.setTextColor(ContextCompat.getColor(this, R.color.warning_text));
            txtVerify.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0);
            if (photoPath != null) showPhoto();
        }
    }

    /** 지우면 되돌릴 수 없으니 한 번 더 묻는다. 원문 사진 파일도 함께 지운다 */
    private void confirmDelete() {
        if (notice == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    Notice n = notice;
                    new Thread(() -> {
                        dao.delete(n); // 할 일은 CASCADE로 함께 지워진다
                        if (n.imagePath != null) new File(n.imagePath).delete();
                        uiHandler.post(this::finish);
                    }).start();
                })
                .show();
    }

    private void showPhoto() {
        btnPhoto.setText(R.string.hide_photo);
        imgOriginal.setVisibility(View.VISIBLE);
        if (imgOriginal.getDrawable() != null) return;
        int width = getResources().getDisplayMetrics().widthPixels;
        // 큰 사진을 줄여 읽는 일은 백그라운드 스레드에서 하고, 화면 반영만 Handler로 메인 스레드에 넘긴다
        new Thread(() -> {
            Bitmap bitmap = loadPhoto(photoPath, width);
            uiHandler.post(() -> {
                if (!isDestroyed() && bitmap != null) imgOriginal.setImageBitmap(bitmap);
            });
        }).start();
    }

    private void hidePhoto() {
        btnPhoto.setText(R.string.show_photo);
        imgOriginal.setVisibility(View.GONE);
    }

    /** 화면 너비에 맞게 줄여 읽고, 카메라가 기록한 방향대로 돌린다 */
    private static Bitmap loadPhoto(String path, int reqWidth) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = 1;
        while (options.outWidth / (options.inSampleSize * 2) >= reqWidth) options.inSampleSize *= 2;
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (bitmap == null) return null;

        int degrees = 0;
        try {
            switch (new ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90: degrees = 90; break;
                case ExifInterface.ORIENTATION_ROTATE_180: degrees = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: degrees = 270; break;
            }
        } catch (IOException e) {
            Log.w(TAG, "사진 방향을 읽지 못함", e);
        }
        if (degrees == 0) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /** 오늘 요청 횟수를 하나 쓰고, 한도를 넘었으면 false */
    private boolean useDailyQuota() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String today = LocalDate.now().toString();
        int count = today.equals(prefs.getString("daily_request_date", ""))
                ? prefs.getInt("daily_request_count", 0) : 0;
        if (count >= DAILY_LIMIT) return false;
        prefs.edit().putString("daily_request_date", today).putInt("daily_request_count", count + 1).apply();
        return true;
    }
}
