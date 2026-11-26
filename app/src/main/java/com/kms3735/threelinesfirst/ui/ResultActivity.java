package com.kms3735.threelinesfirst.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.kms3735.threelinesfirst.BuildConfig;
import com.kms3735.threelinesfirst.R;
import com.kms3735.threelinesfirst.ai.Summary;
import com.kms3735.threelinesfirst.ai.SummaryClient;
import com.kms3735.threelinesfirst.data.AppDatabase;
import com.kms3735.threelinesfirst.data.Notice;
import com.kms3735.threelinesfirst.data.NoticeDao;
import com.kms3735.threelinesfirst.data.Todo;
import com.kms3735.threelinesfirst.util.DeadlineAlarm;
import com.kms3735.threelinesfirst.util.KeepWords;
import com.kms3735.threelinesfirst.verify.Verifier;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ③ 세 줄 요약: 개인정보를 가린 글을 AI로 요약해 세 줄, 경고, 할 일 체크박스로 보여 준다.
 * 한 화면이 길어지지 않게 1 요약 / 2 할 일·바로 하기 / 3 원문 세 쪽으로 나눠 탭과 아래 버튼으로 넘긴다.
 * 요약 속 날짜·금액은 원문과 대조해, 다르면 한 번 다시 요약하고 그래도 다르면 경고 배지를 붙인다.
 * 안내문은 Room에 저장돼 있고, 이미 요약한 안내문은 AI를 다시 부르지 않고 저장된 요약을 보여 준다.
 * ④ 행동 연결: 읽어 주기, 달력, 전화, 가족에게 보내기, 장소 지도. 달력·전화·공유는 다른 앱에 맡겨 권한이 필요 없다.
 */
public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    public static final String EXTRA_NOTICE_ID = "notice_id";

    /** 무료 사용량을 지키려고 하루 요청 횟수를 제한한다 */
    private static final int DAILY_LIMIT = 30;

    private TextView txtStatus, txtVerify, txtLine1, txtLine2, txtLine3, txtWarning, txtTodoCount, txtAlarm;
    private MaterialButton btnRetry, btnPhoto, btnLeft, btnRight;
    private final MaterialButton[] tabs = new MaterialButton[3];
    private final View[] pages = new View[3];
    private ImageView imgOriginal;
    private View statusGroup, todoHeader;
    private LinearLayout todoGroup, actionGrid;
    private ScrollView scroll;

    /** 지금 보는 쪽 (0: 요약, 1: 할 일, 2: 원문) */
    private int page;
    /** 요약을 보여 줄 수 있는지. 아니면 1, 2쪽 자리에 요약 중·실패 안내를 보여 준다 */
    private boolean ready;

    private NoticeDao dao;
    private Notice notice;
    private String maskedText;
    /** 원문 사진 경로. 공유받은 글처럼 사진이 없으면 null */
    private String photoPath;
    private SummaryClient client;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    /** 대조에 실패해 이미 한 번 다시 요약했는지 */
    private boolean resummarized;

    private TextToSpeech tts;
    /** 한국어 음성이 준비됐는지. 처음 누를 때 엔진을 켠다 */
    private boolean ttsReady;

    /** 마감일 알림을 띄우려면 Android 13부터 권한이 필요하다. 거절해도 요약은 그대로 쓴다 */
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) Toast.makeText(this, R.string.alarm_denied, Toast.LENGTH_LONG).show();
            });

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
        KeepWords.applyTo(findViewById(R.id.main));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtStatus = findViewById(R.id.txtStatus);
        btnRetry = findViewById(R.id.btnRetry);
        statusGroup = findViewById(R.id.statusGroup);
        txtVerify = findViewById(R.id.txtVerify);
        txtLine1 = findViewById(R.id.txtLine1);
        txtLine2 = findViewById(R.id.txtLine2);
        txtLine3 = findViewById(R.id.txtLine3);
        btnPhoto = findViewById(R.id.btnPhoto);
        imgOriginal = findViewById(R.id.imgOriginal);
        txtWarning = findViewById(R.id.txtWarning);
        todoHeader = findViewById(R.id.todoHeader);
        txtTodoCount = findViewById(R.id.txtTodoCount);
        todoGroup = findViewById(R.id.todoGroup);
        actionGrid = findViewById(R.id.actionGrid);
        txtAlarm = findViewById(R.id.txtAlarm);
        scroll = findViewById(R.id.scroll);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        tabs[0] = findViewById(R.id.tab1);
        tabs[1] = findViewById(R.id.tab2);
        tabs[2] = findViewById(R.id.tab3);
        pages[0] = findViewById(R.id.page1);
        pages[1] = findViewById(R.id.page2);
        pages[2] = findViewById(R.id.page3);

        for (int i = 0; i < tabs.length; i++) {
            int p = i;
            tabs[i].setOnClickListener(v -> showPage(p));
        }
        // 1쪽: 듣기 / 다음: 할 일, 2쪽: 이전 / 다음: 원문 확인, 3쪽: 이전 / 다 봤어요, 목록으로
        btnLeft.setOnClickListener(v -> {
            if (page == 0) speak();
            else showPage(page - 1);
        });
        btnRight.setOnClickListener(v -> {
            if (page < 2) showPage(page + 1);
            else goHome();
        });
        findViewById(R.id.btnList).setOnClickListener(v -> goHome());
        txtVerify.setOnClickListener(v -> showPage(2));
        ((TextView) findViewById(R.id.txtNote)).setText(KeepWords.of(getString(R.string.summary_note)));
        btnPhoto.setOnClickListener(v -> togglePhotoSize());
        btnRetry.setOnClickListener(v -> summarize());
        showPage(0);

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
        showOriginal();
        photoPath = n.imagePath;
        if (photoPath != null && !new File(photoPath).exists()) photoPath = null;
        findViewById(R.id.photoGroup).setVisibility(photoPath != null ? View.VISIBLE : View.GONE);
        if (photoPath != null) loadPhoto();

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
            DeadlineAlarm.schedule(this, notice);
            List<Todo> saved = dao.getTodos(notice.id);
            uiHandler.post(() -> {
                if (isDestroyed()) return;
                showSummary(saved);
                askNotificationPermission();
            });
        }).start();
    }

    /** 마감일이 있는 안내문을 처음 요약했을 때만 묻는다 */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !notice.deadline.isEmpty()
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void showStatus(int messageRes, boolean canRetry) {
        txtStatus.setText(messageRes);
        btnRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
        ready = false;
        showPage(page);
    }

    private void goHome() {
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    /** 고른 쪽만 보이고, 탭 색과 아래 버튼 두 개의 글자를 그 쪽에 맞게 바꾼다 */
    private void showPage(int p) {
        if (p != page && tts != null && tts.isSpeaking()) tts.stop();
        page = p;
        int accent = ContextCompat.getColor(this, R.color.accent);
        for (int i = 0; i < tabs.length; i++) {
            boolean on = i == p;
            tabs[i].setBackgroundTintList(ColorStateList.valueOf(on ? accent : Color.TRANSPARENT));
            tabs[i].setTextColor(ContextCompat.getColor(this, on ? R.color.on_accent : R.color.body));
            tabs[i].setSelected(on);
        }
        pages[0].setVisibility(p == 0 && ready ? View.VISIBLE : View.GONE);
        pages[1].setVisibility(p == 1 && ready ? View.VISIBLE : View.GONE);
        pages[2].setVisibility(p == 2 ? View.VISIBLE : View.GONE);
        statusGroup.setVisibility(p < 2 && !ready ? View.VISIBLE : View.GONE);

        if (p == 0) {
            setButton(btnLeft, R.string.action_speak, R.drawable.ic_speaker);
            btnLeft.setVisibility(ready ? View.VISIBLE : View.GONE);
            setButton(btnRight, R.string.next_todo, R.drawable.ic_arrow_right);
        } else {
            setButton(btnLeft, R.string.prev, R.drawable.ic_chevron_left);
            btnLeft.setVisibility(View.VISIBLE);
            if (p == 1) setButton(btnRight, R.string.next_original, R.drawable.ic_arrow_right);
            else setButton(btnRight, R.string.done_to_list, 0);
        }
        scroll.scrollTo(0, 0);
    }

    private static void setButton(MaterialButton button, int textRes, int iconRes) {
        button.setText(textRes);
        button.setIconResource(iconRes);
    }

    private void showSummary(List<Todo> todos) {
        Notice n = notice;
        ready = true;
        showPage(page);

        showVerifyBadge(n.checkedCount, n.mismatches);
        txtLine1.setText(KeepWords.of(n.line1What));
        txtLine2.setText(KeepWords.of(n.line2Todo));
        txtLine3.setText(KeepWords.of(n.line3WhenWhere));
        String warning = n.warning == null ? "" : n.warning;
        txtWarning.setText(KeepWords.of(warning));
        txtWarning.setVisibility(warning.trim().isEmpty() ? View.GONE : View.VISIBLE);

        // 체크하면 바로 저장해서, 앱을 다시 켜도 체크 상태가 남는다
        todoGroup.removeAllViews();
        for (Todo todo : todos) {
            CheckBox box = (CheckBox) getLayoutInflater().inflate(R.layout.item_todo, todoGroup, false);
            box.setText(KeepWords.of(todo.content));
            box.setChecked(todo.done);
            box.setOnCheckedChangeListener((b, checked) -> {
                todo.done = checked;
                updateTodoCount(todos);
                new Thread(() -> dao.updateTodo(todo)).start();
            });
            todoGroup.addView(box);
        }
        todoHeader.setVisibility(todos.isEmpty() ? View.GONE : View.VISIBLE);
        updateTodoCount(todos);

        // 바로 하기: 해당하는 값이 있는 칸만 두 개씩 줄지어 넣는다
        boolean hasDeadline = n.deadline != null && !n.deadline.isEmpty();
        boolean hasPhone = n.phone != null && !n.phone.isEmpty();
        boolean hasPlace = n.place != null && !n.place.isEmpty() && BuildConfig.HAS_MAPS_KEY;
        actionGrid.removeAllViews();
        if (hasDeadline) addTile(R.drawable.ic_calendar, getString(R.string.action_calendar), v -> addToCalendar());
        if (hasPhone) addTile(R.drawable.ic_phone, getString(R.string.action_dial, n.phone),
                v -> open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + n.phone))));
        addTile(R.drawable.ic_family, getString(R.string.action_share), v -> share());
        if (hasPlace) addTile(R.drawable.ic_pin, getString(R.string.action_map, n.place),
                v -> startActivity(new Intent(this, MapActivity.class)
                        .putExtra(MapActivity.EXTRA_PLACE, n.place)
                        .putExtra(MapActivity.EXTRA_PHONE, hasPhone ? n.phone : "")));

        // 마감일 알림 안내. 알림을 꺼 두었거나 이미 지난 날이면 숨긴다
        List<LocalDate> days = DeadlineAlarm.upcomingDays(n);
        boolean alarmOn = !days.isEmpty() && NotificationManagerCompat.from(this).areNotificationsEnabled();
        txtAlarm.setVisibility(alarmOn ? View.VISIBLE : View.GONE);
        if (days.size() == 1) txtAlarm.setText(getString(R.string.alarm_info_one, monthDay(days.get(0))));
        else if (days.size() >= 2) txtAlarm.setText(getString(R.string.alarm_info_two, monthDay(days.get(0)), monthDay(days.get(1))));
    }

    private String monthDay(LocalDate d) {
        return getString(R.string.date_md, d.getMonthValue(), d.getDayOfMonth());
    }

    private void updateTodoCount(List<Todo> todos) {
        int done = 0;
        for (Todo t : todos) if (t.done) done++;
        txtTodoCount.setText(getString(R.string.todo_done_count, todos.size(), done));
    }

    /** 바로 하기 칸 하나를 넣는다. 둘째 줄(전화번호, 장소)은 작고 흐리게 */
    private void addTile(int iconRes, String text, View.OnClickListener onClick) {
        LinearLayout row = actionGrid.getChildCount() == 0 ? null
                : (LinearLayout) actionGrid.getChildAt(actionGrid.getChildCount() - 1);
        if (row == null || row.getChildCount() == 2) {
            row = new LinearLayout(this);
            row.setBaselineAligned(false); // 둘째 줄이 있는 칸과 없는 칸의 높이와 위치를 맞춘다
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = actionGrid.getChildCount() == 0 ? 0 : getResources().getDimensionPixelSize(R.dimen.tile_gap);
            actionGrid.addView(row, lp);
        }
        MaterialButton tile = (MaterialButton) getLayoutInflater().inflate(R.layout.item_tile, row, false);
        int br = text.indexOf('\n');
        if (br < 0) {
            tile.setText(text);
        } else {
            SpannableString s = new SpannableString(text);
            s.setSpan(new RelativeSizeSpan(0.8f), br, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.sub)), br, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tile.setText(s);
        }
        tile.setIconResource(iconRes);
        tile.setOnClickListener(onClick);
        if (row.getChildCount() == 1) {
            ((LinearLayout.LayoutParams) tile.getLayoutParams()).setMarginStart(getResources().getDimensionPixelSize(R.dimen.tile_gap));
        }
        row.addView(tile);
    }

    /** 세 줄과 주의할 점을 천천히 읽어 준다. 읽는 중에 누르면 멈춘다 */
    private void speak() {
        if (tts == null) {
            tts = new TextToSpeech(this, status -> {
                ttsReady = status == TextToSpeech.SUCCESS
                        && tts.setLanguage(Locale.KOREAN) >= TextToSpeech.LANG_AVAILABLE;
                if (ttsReady) speakSummary();
                else Toast.makeText(this, R.string.tts_unavailable, Toast.LENGTH_LONG).show();
            });
            tts.setSpeechRate(0.9f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {
                }

                @Override
                public void onDone(String id) {
                    uiHandler.post(ResultActivity.this::resetSpeakButton);
                }

                @Override
                public void onError(String id) {
                    uiHandler.post(ResultActivity.this::resetSpeakButton);
                }
            });
            return;
        }
        if (!ttsReady) {
            Toast.makeText(this, R.string.tts_unavailable, Toast.LENGTH_LONG).show();
        } else if (tts.isSpeaking()) {
            tts.stop();
            resetSpeakButton();
        } else {
            speakSummary();
        }
    }

    private void speakSummary() {
        String text = notice.line1What + "\n" + notice.line2Todo + "\n" + notice.line3WhenWhere;
        if (notice.warning != null && !notice.warning.trim().isEmpty()) text += "\n" + notice.warning;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "summary");
        if (page == 0) btnLeft.setText(R.string.action_stop_speak);
    }

    /** 다 읽었거나 멈췄을 때. 다른 쪽으로 넘어갔으면 그 쪽 버튼 글자를 건드리지 않는다 */
    private void resetSpeakButton() {
        if (page == 0) btnLeft.setText(R.string.action_speak);
    }

    /** 달력 앱의 새 일정 화면을 채워 연다. 저장은 사용자가 달력 앱에서 누른다 */
    private void addToCalendar() {
        long begin = LocalDate.parse(notice.deadline).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        open(new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, getString(R.string.calendar_title, notice.line1What))
                .putExtra(CalendarContract.Events.DESCRIPTION, notice.line2Todo + "\n" + notice.line3WhenWhere)
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin + 24 * 60 * 60 * 1000L));
    }

    /** 세 줄 요약 글과 원문 사진을 가족에게 보낸다. 받는 앱과 사람은 사용자가 고른다 */
    private void share() {
        StringBuilder text = new StringBuilder()
                .append(notice.line1What).append('\n')
                .append(notice.line2Todo).append('\n')
                .append(notice.line3WhenWhere);
        if (notice.warning != null && !notice.warning.trim().isEmpty()) text.append('\n').append(notice.warning);
        text.append("\n\n").append(getString(R.string.share_footer));

        Intent send = new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, text.toString());
        if (photoPath != null) {
            Uri photo = FileProvider.getUriForFile(this, getPackageName() + ".files", new File(photoPath));
            send.setType("image/*")
                    .putExtra(Intent.EXTRA_STREAM, photo)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            send.setClipData(ClipData.newRawUri("", photo));
        } else {
            send.setType("text/plain");
        }
        open(Intent.createChooser(send, getString(R.string.share_title)));
    }

    /** 다른 앱을 연다. 해당 앱이 없으면 안내만 한다 */
    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_app, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.shutdown();
    }

    /** 3쪽 원문 글: 날짜·금액을 노랗게 칠해 요약과 맞춰 보기 쉽게 한다 */
    private void showOriginal() {
        int mark = ContextCompat.getColor(this, R.color.mark);
        SpannableString text = new SpannableString(maskedText);
        List<int[]> spans = Verifier.valueSpans(maskedText);
        for (int[] s : spans) {
            text.setSpan(new BackgroundColorSpan(mark), s[0], s[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ((TextView) findViewById(R.id.txtOriginal)).setText(text);

        TextView note = findViewById(R.id.txtMarkNote);
        note.setVisibility(spans.isEmpty() ? View.GONE : View.VISIBLE);
        // 낱말 단위 줄바꿈 글자를 넣은 뒤에 자리를 찾아야 칠할 곳이 어긋나지 않는다
        String noteText = KeepWords.of(getString(R.string.original_mark_note));
        String marked = KeepWords.of(getString(R.string.original_mark_word));
        SpannableString noteSpan = new SpannableString(noteText);
        int at = noteText.indexOf(marked);
        if (at >= 0) noteSpan.setSpan(new BackgroundColorSpan(mark), at, at + marked.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        note.setText(noteSpan);
    }

    /** 통과면 초록 체크, 다르면 주황 경고(누르면 3쪽 원문으로). 대조할 값이 없으면 숨긴다 */
    private void showVerifyBadge(int checked, String mismatches) {
        // 요약에만 있고 원문에는 없는 값은 원문 글 안에 칠할 자리가 없으므로, 3쪽 원문 글 위에 따로 알린다
        TextView missing = findViewById(R.id.txtOriginalMissing);
        boolean hasMissing = mismatches != null && !mismatches.isEmpty();
        missing.setVisibility(hasMissing ? View.VISIBLE : View.GONE);
        if (hasMissing) missing.setText(KeepWords.of(getString(R.string.original_missing, mismatches)));
        if (checked == 0) {
            txtVerify.setVisibility(View.GONE);
            return;
        }
        txtVerify.setVisibility(View.VISIBLE);
        boolean passed = mismatches == null || mismatches.isEmpty();
        txtVerify.setClickable(!passed);
        if (passed) {
            txtVerify.setText(R.string.verified);
            txtVerify.setBackgroundResource(R.drawable.verified_box);
            txtVerify.setTextColor(ContextCompat.getColor(this, R.color.verified_text));
            txtVerify.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
        } else {
            txtVerify.setText(getString(photoPath != null ? R.string.verify_failed : R.string.verify_failed_no_photo, mismatches));
            txtVerify.setBackgroundResource(R.drawable.warning_box);
            txtVerify.setTextColor(ContextCompat.getColor(this, R.color.warning_text));
            txtVerify.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0);
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
                        DeadlineAlarm.cancel(this, n.id);
                        if (n.imagePath != null) new File(n.imagePath).delete();
                        uiHandler.post(this::finish);
                    }).start();
                })
                .show();
    }

    /** 사진은 처음에 일부만 잘라 보여 주고, 크게 보기를 누르면 전체를 펼친다 */
    private void togglePhotoSize() {
        boolean small = imgOriginal.getScaleType() == ImageView.ScaleType.CENTER_CROP;
        ViewGroup.LayoutParams lp = imgOriginal.getLayoutParams();
        lp.height = small ? ViewGroup.LayoutParams.WRAP_CONTENT : getResources().getDimensionPixelSize(R.dimen.photo_small);
        imgOriginal.setLayoutParams(lp);
        imgOriginal.setAdjustViewBounds(small);
        imgOriginal.setScaleType(small ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP);
        btnPhoto.setText(small ? R.string.hide_photo : R.string.show_photo);
    }

    private void loadPhoto() {
        int width = getResources().getDisplayMetrics().widthPixels;
        // 큰 사진을 줄여 읽는 일은 백그라운드 스레드에서 하고, 화면 반영만 Handler로 메인 스레드에 넘긴다
        new Thread(() -> {
            Bitmap bitmap = loadPhoto(photoPath, width);
            uiHandler.post(() -> {
                if (!isDestroyed() && bitmap != null) imgOriginal.setImageBitmap(bitmap);
            });
        }).start();
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
