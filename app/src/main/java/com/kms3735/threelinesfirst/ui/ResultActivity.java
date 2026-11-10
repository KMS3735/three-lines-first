package com.kms3735.threelinesfirst.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kms3735.threelinesfirst.BuildConfig;
import com.kms3735.threelinesfirst.R;
import com.kms3735.threelinesfirst.ai.Summary;
import com.kms3735.threelinesfirst.ai.SummaryClient;

import java.time.LocalDate;

/** ③ 세 줄 요약: 개인정보를 가린 글을 AI로 요약해 세 줄, 경고, 할 일 체크박스로 보여 준다 */
public class ResultActivity extends AppCompatActivity {

    // ponytail: 5단계(Room)부터는 글 대신 notice_id만 넘기고 DB에서 다시 읽는다
    public static final String EXTRA_MASKED_TEXT = "masked_text";

    /** 무료 사용량을 지키려고 하루 요청 횟수를 제한한다 */
    private static final int DAILY_LIMIT = 30;

    private TextView txtStatus, txtLine1, txtLine2, txtLine3, txtWarning, txtTodosTitle;
    private Button btnRetry;
    private View summaryGroup;
    private LinearLayout todoGroup;

    private String maskedText;
    private SummaryClient client;

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
        txtLine1 = findViewById(R.id.txtLine1);
        txtLine2 = findViewById(R.id.txtLine2);
        txtLine3 = findViewById(R.id.txtLine3);
        txtWarning = findViewById(R.id.txtWarning);
        txtTodosTitle = findViewById(R.id.txtTodosTitle);
        todoGroup = findViewById(R.id.todoGroup);

        maskedText = getIntent().getStringExtra(EXTRA_MASKED_TEXT);
        if (maskedText == null) maskedText = "";
        ((TextView) findViewById(R.id.txtOriginal)).setText(maskedText);

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
        client.summarize(maskedText, LocalDate.now(), new SummaryClient.Callback() {
            @Override
            public void onSuccess(Summary summary) {
                if (!isDestroyed()) showSummary(summary);
            }

            @Override
            public void onFailure() {
                if (!isDestroyed()) showStatus(R.string.summary_error, true);
            }
        });
    }

    private void showStatus(int messageRes, boolean canRetry) {
        txtStatus.setText(messageRes);
        txtStatus.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
        summaryGroup.setVisibility(View.GONE);
    }

    private void showSummary(Summary s) {
        txtStatus.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        summaryGroup.setVisibility(View.VISIBLE);

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
