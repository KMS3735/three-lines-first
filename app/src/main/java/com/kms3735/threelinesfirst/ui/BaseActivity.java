package com.kms3735.threelinesfirst.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.kms3735.threelinesfirst.util.KeepWords;

/**
 * 모든 화면의 부모. 큰 글씨 설정이 켜져 있으면 글자 배율(fontScale)을 키워 sp로 정한 글자를 한꺼번에 키운다.
 * 휴대폰 설정의 글자 크기 위에 곱하므로 사용자가 고른 크기도 그대로 살아 있다.
 * 화면의 모든 글은 낱말 중간에서 줄이 바뀌지 않게 한다 (KeepWords).
 */
public class BaseActivity extends AppCompatActivity {

    /** 본문 20sp를 큰 글씨 기준 28sp로 */
    private static final float LARGE_SCALE = 1.4f;

    /** 이 화면을 만들 때의 큰 글씨 설정. 설정 화면에서 바꾸고 돌아오면 다시 그린다 */
    private boolean largeText;

    @Override
    protected void attachBaseContext(Context base) {
        largeText = SettingsActivity.largeText(base);
        if (largeText) {
            Configuration config = new Configuration(base.getResources().getConfiguration());
            config.fontScale *= LARGE_SCALE;
            base = base.createConfigurationContext(config);
        }
        super.attachBaseContext(base);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        KeepWords.watch(getWindow().getDecorView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (largeText != SettingsActivity.largeText(this)) recreate();
    }
}
