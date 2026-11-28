package com.kms3735.threelinesfirst.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CompoundButton;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kms3735.threelinesfirst.R;

/**
 * ⑤ 설정: 큰 글씨, 쉬운 말, 원문 사진 보관, 마감일 알림. 한 번 정하면 모든 안내문에 적용된다.
 * 큰 글씨는 바로 이 화면부터 커지고, 쉬운 말은 다음에 요약하는 안내문부터 적용된다.
 */
public class SettingsActivity extends BaseActivity {

    private static final String PREFS = "settings";
    private static final String KEY_LARGE_TEXT = "large_text";
    private static final String KEY_EASY_MODE = "easy_mode";
    private static final String KEY_KEEP_PHOTO = "keep_photo";

    /** 기본은 보관. 끄면 요약한 뒤 원문 사진을 지운다 (원문과 다른 값이 있는 안내문은 남긴다) */
    public static boolean keepPhoto(Context context) {
        return prefs(context).getBoolean(KEY_KEEP_PHOTO, true);
    }

    public static boolean largeText(Context context) {
        return prefs(context).getBoolean(KEY_LARGE_TEXT, false);
    }

    public static boolean easyMode(Context context) {
        return prefs(context).getBoolean(KEY_EASY_MODE, false);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        CompoundButton large = findViewById(R.id.switchLarge);
        large.setChecked(largeText(this));
        large.setOnCheckedChangeListener((b, on) -> {
            prefs(this).edit().putBoolean(KEY_LARGE_TEXT, on).apply();
            recreate(); // 바뀐 크기를 이 화면에서 바로 보여 준다
        });

        CompoundButton easy = findViewById(R.id.switchEasy);
        easy.setChecked(easyMode(this));
        easy.setOnCheckedChangeListener((b, on) -> prefs(this).edit().putBoolean(KEY_EASY_MODE, on).apply());

        CompoundButton photo = findViewById(R.id.switchPhoto);
        photo.setChecked(keepPhoto(this));
        photo.setOnCheckedChangeListener((b, on) -> prefs(this).edit().putBoolean(KEY_KEEP_PHOTO, on).apply());

        // 알림을 켜고 끄는 곳은 휴대폰 설정이 맡는다 (앱에서 따로 막으면 두 곳을 맞춰야 한다)
        findViewById(R.id.btnAlarm).setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
    }
}
