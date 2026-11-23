package com.kms3735.threelinesfirst.ui;

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
import com.kms3735.threelinesfirst.verify.Verifier;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

/**
 * ③ 세 줄 요약: 개인정보를 가린 글을 AI로 요약해 세 줄, 경고, 할 일 체크박스로 보여 준다.
 * 요약 속 날짜·금액은 원문과 대조해, 다르면 한 번 다시 요약하고 그래도 다르면 경고 배지를 붙인다.
 */
public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    // ponytail: 5단계(Room)부터는 글 대신 notice_id만 넘기고 DB에서 다시 읽는다
    public static final String EXTRA_MASKED_TEXT = "masked_text";
    /** 원문 사진 경로. 공유받은 글처럼 사진이 없으면 null */
    public static final String EXTRA_PHOTO_PATH = "photo_path";

    /** 무료 사용량을 지키려고 하루 요청 횟수를 제한한다 */
    private static final int DAILY_LIMIT = 30;

    private TextView txtStatus, txtVerify, txtLine1, txtLine2, txtLine3, txtWarning, txtTodosTitle;
    private Button btnRetry, btnPhoto;
    private ImageView imgOriginal;
    private View summaryGroup;
    private LinearLayout todoGroup;

    private String maskedText;
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

        maskedText = getIntent().getStringExtra(EXTRA_MASKED_TEXT);
        if (maskedText == null) maskedText = "";
        ((TextView) findViewById(R.id.txtOriginal)).setText(maskedText);

        photoPath = getIntent().getStringExtra(EXTRA_PHOTO_PATH);
        if (photoPath != null && !new File(photoPath).exists()) photoPath = null;
        btnPhoto.setVisibility(photoPath != null ? View.VISIBLE : View.GONE);
        btnPhoto.setOnClickListener(v -> {
            if (imgOriginal.getVisibility() == View.VISIBLE) hidePhoto();
            else showPhoto();
        });

        btnRetry.setOnClickListener(v -> summarize());
        findViewById(R.id.btnHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        client = new SummaryClient(BuildConfig.GEMINI_API_KEY);
        summarize();
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
        showSummary(summary, result);
    }

    private void showStatus(int messageRes, boolean canRetry) {
        txtStatus.setText(messageRes);
        txtStatus.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
        summaryGroup.setVisibility(View.GONE);
    }

    private void showSummary(Summary s, Verifier.Result result) {
        txtStatus.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        summaryGroup.setVisibility(View.VISIBLE);

        showVerifyBadge(result);
        txtLine1.setText(s.line1What);
        txtLine2.setText(s.line2Todo);
        txtLine3.setText(s.line3WhenWhere);
        txtWarning.setText(s.warning);
        txtWarning.setVisibility(s.warning.trim().isEmpty() ? View.GONE : View.VISIBLE);

        // ponytail: 체크 상태는 5단계에서 Room의 Todo에 저장한다
        todoGroup.removeAllViews();
        float textPx = getResources().getDimension(R.dimen.text_body);
        int minHeight = getResources().getDimensionPixelSize(R.dimen.touch_min);
        for (String todo : s.todos) {
            CheckBox box = new CheckBox(this);
            box.setText(todo);
            box.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
            box.setMinHeight(minHeight);
            todoGroup.addView(box);
        }
        txtTodosTitle.setVisibility(s.todos.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** 통과면 초록 체크, 다르면 주황 경고와 함께 원문 사진을 펼친다. 대조할 값이 없으면 숨긴다 */
    private void showVerifyBadge(Verifier.Result result) {
        if (result.checked == 0) {
            txtVerify.setVisibility(View.GONE);
            return;
        }
        txtVerify.setVisibility(View.VISIBLE);
        if (result.passed()) {
            txtVerify.setText(R.string.verified);
            txtVerify.setBackgroundResource(R.drawable.verified_box);
            txtVerify.setTextColor(ContextCompat.getColor(this, R.color.verified_text));
            txtVerify.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
        } else {
            String values = String.join(", ", result.mismatches);
            txtVerify.setText(getString(photoPath != null ? R.string.verify_failed : R.string.verify_failed_no_photo, values));
            txtVerify.setBackgroundResource(R.drawable.warning_box);
            txtVerify.setTextColor(ContextCompat.getColor(this, R.color.warning_text));
            txtVerify.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0);
            if (photoPath != null) showPhoto();
        }
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
