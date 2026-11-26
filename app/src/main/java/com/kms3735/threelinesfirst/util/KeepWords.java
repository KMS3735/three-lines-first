package com.kms3735.threelinesfirst.util;

import android.os.Build;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 한국어를 낱말 단위로 줄바꿈해 "안내문이 / 에요."처럼 글자 중간에서 끊기지 않게 한다.
 * Android 13 이상은 테마의 lineBreakWordStyle=phrase가 처리하고,
 * 그 아래에서는 낱말 안 글자 사이에 WORD JOINER(U+2060, 보이지 않는 글자)를 넣어 띄어쓰기에서만 줄이 바뀌게 한다.
 * 화면에 보여 줄 때만 쓰고, 저장·공유·읽어 주기에는 원래 글을 쓴다.
 */
public final class KeepWords {

    private static final char WORD_JOINER = '⁠';

    private KeepWords() {
    }

    public static String of(String s) {
        if (s == null || Build.VERSION.SDK_INT >= 33) return s;
        return join(s);
    }

    /** 레이아웃에 적어 둔 글(동의 화면 설명, 항목 이름 등)에도 적용한다. setContentView 다음에 부른다 */
    public static void applyTo(View v) {
        if (Build.VERSION.SDK_INT >= 33) return;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) applyTo(g.getChildAt(i));
        } else if (v instanceof TextView && !(v instanceof EditText)) {
            TextView t = (TextView) v;
            // 복사할 수 있는 원문 글과 꾸밈이 들어간 글은 그대로 둔다
            if (!t.isTextSelectable() && !(t.getText() instanceof Spanned)) t.setText(join(t.getText().toString()));
        }
    }

    static String join(String s) {
        StringBuilder b = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i > 0 && !Character.isWhitespace(c) && !Character.isWhitespace(s.charAt(i - 1))
                    && !Character.isLowSurrogate(c)) {
                b.append(WORD_JOINER);
            }
            b.append(c);
        }
        return b.toString();
    }
}
