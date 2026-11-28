package com.kms3735.threelinesfirst.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kms3735.threelinesfirst.R;
import com.kms3735.threelinesfirst.data.AppDatabase;
import com.kms3735.threelinesfirst.data.Notice;
import com.kms3735.threelinesfirst.data.NoticeItem;
import com.kms3735.threelinesfirst.util.KeepWords;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * ① 안내문 목록. Room에서 읽어 마감이 가까운 순으로 보여 준다.
 * 마감이 남은 것 중 가장 가까운 안내문은 위쪽 파란 카드로 따로 꺼내 보여 준다.
 */
public class MainActivity extends BaseActivity {

    private NoticeAdapter adapter;
    private TextView txtEmpty, txtHeadline, txtOthers, txtHeroTitle, txtHeroWhen;
    private View heroCard, counts;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    /** 사진 선택기: 사용자가 고른 사진 한 장에만 접근하므로 저장소 권한이 필요 없다 */
    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                Intent intent = new Intent(this, CaptureActivity.class);
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            });

    /** 첫 실행 개인정보 동의. 동의하지 않으면 앱을 닫는다 */
    private final ActivityResultLauncher<Intent> consent =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    Toast.makeText(this, R.string.consent_needed, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtEmpty = findViewById(R.id.txtEmpty);
        txtHeadline = findViewById(R.id.txtHeadline);
        txtOthers = findViewById(R.id.txtOthers);
        txtHeroTitle = findViewById(R.id.txtHeroTitle);
        txtHeroWhen = findViewById(R.id.txtHeroWhen);
        heroCard = findViewById(R.id.heroCard);
        counts = findViewById(R.id.counts);

        RecyclerView noticeList = findViewById(R.id.noticeList);
        adapter = new NoticeAdapter(this, this::open);
        noticeList.setLayoutManager(new LinearLayoutManager(this));
        noticeList.setAdapter(adapter);

        findViewById(R.id.btnCapture).setOnClickListener(v ->
                startActivity(new Intent(this, CaptureActivity.class)));
        findViewById(R.id.btnImport).setOnClickListener(v ->
                pickPhoto.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        if (savedInstanceState == null && !ConsentActivity.isAgreed(this)) {
            consent.launch(new Intent(this, ConsentActivity.class));
        }
    }

    private void open(Notice notice) {
        startActivity(new Intent(this, ResultActivity.class).putExtra(ResultActivity.EXTRA_NOTICE_ID, notice.id));
    }

    /** 요약 화면에서 체크하거나 지우고 돌아오면 바로 반영되도록 화면에 보일 때마다 다시 읽는다 */
    @Override
    protected void onResume() {
        super.onResume();
        new Thread(() -> {
            List<NoticeItem> items = AppDatabase.get(this).noticeDao().getList();
            uiHandler.post(() -> {
                if (!isDestroyed()) show(items);
            });
        }).start();
    }

    /** 0: 마감이 남음, 1: 마감일 없음·요약 전, 2: 마감 지남 */
    private static int group(NoticeItem item) {
        Long left = NoticeAdapter.daysLeft(item.notice);
        if (left == null) return 1;
        return left >= 0 ? 0 : 2;
    }

    private void show(List<NoticeItem> items) {
        // 마감이 남은 것은 가까운 순, 지난 것은 최근에 지난 순. 마감일 없는 것은 찍은 순서 그대로 (List.sort는 안정 정렬)
        items.sort(Comparator.comparingInt(MainActivity::group).thenComparingLong(i -> {
            Long left = NoticeAdapter.daysLeft(i.notice);
            return left == null ? 0 : Math.abs(left);
        }));

        int urgent = 0, active = 0, passed = 0;
        for (NoticeItem i : items) {
            Long left = NoticeAdapter.daysLeft(i.notice);
            if (left == null || left > NoticeAdapter.URGENT_DAYS) active++;
            else if (left >= 0) urgent++;
            else passed++;
        }

        ((TextView) findViewById(R.id.txtGreeting)).setText(getString(R.string.greeting_line, greeting(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))));
        txtHeadline.setText(headline(items.size(), urgent + active));
        txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        counts.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        setCount(R.id.txtCountUrgent, urgent, R.string.count_urgent, R.color.warning_text);
        setCount(R.id.txtCountActive, active, R.string.count_active, R.color.ink);
        setCount(R.id.txtCountPassed, passed, R.string.count_passed, R.color.sub);

        // 맨 앞이 마감이 남은 안내문이면 파란 카드로 꺼낸다
        NoticeItem hero = !items.isEmpty() && group(items.get(0)) == 0 ? items.remove(0) : null;
        heroCard.setVisibility(hero == null ? View.GONE : View.VISIBLE);
        if (hero != null) {
            Notice n = hero.notice;
            LocalDate d = LocalDate.parse(n.deadline);
            txtHeroTitle.setText(KeepWords.of(n.line1What));
            txtHeroWhen.setText(getString(R.string.hero_when,
                    getString(R.string.date_md, d.getMonthValue(), d.getDayOfMonth()),
                    NoticeAdapter.leftText(this, NoticeAdapter.daysLeft(n))));
            heroCard.setOnClickListener(v -> open(n));
        }

        adapter.setItems(items);
        txtOthers.setVisibility(hero != null && !items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return getString(R.string.greeting_morning);
        if (hour < 18) return getString(R.string.greeting_afternoon);
        return getString(R.string.greeting_evening);
    }

    /** "챙길 안내문이 3개 있어요"에서 "3개"만 파란색으로 */
    private CharSequence headline(int total, int toDo) {
        if (total == 0) return getString(R.string.headline_empty);
        if (toDo == 0) return getString(R.string.headline_none);
        String text = getString(R.string.headline_count, toDo);
        String number = toDo + "개";
        int start = text.indexOf(number);
        SpannableString s = new SpannableString(text);
        if (start >= 0) {
            s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.accent_text)),
                    start, start + number.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    /** 숫자는 크고 굵게, 이름은 그 아래에 */
    private void setCount(int viewId, int count, int labelRes, int colorRes) {
        String number = String.valueOf(count);
        SpannableString s = new SpannableString(number + "\n" + getString(labelRes));
        int end = number.length();
        s.setSpan(new RelativeSizeSpan(1.3f), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, colorRes)), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) findViewById(viewId)).setText(s);
    }
}
