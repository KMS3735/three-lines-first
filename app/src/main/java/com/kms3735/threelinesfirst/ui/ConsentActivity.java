package com.kms3735.threelinesfirst.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kms3735.threelinesfirst.R;

/** 첫 실행 개인정보 동의. 어떤 정보가 외부 AI로 가는지 알리고 동의를 받는다. */
public class ConsentActivity extends AppCompatActivity {

    private static final String PREFS = "settings";
    private static final String KEY_AGREED = "consent_agreed";

    public static boolean isAgreed(Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_AGREED, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_consent);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnAgree).setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_AGREED, true).apply();
            setResult(RESULT_OK);
            finish();
        });
        findViewById(R.id.btnDisagree).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}
