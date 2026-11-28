package com.kms3735.threelinesfirst.util;

import android.os.Build;
import android.text.SpannableStringBuilder;
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

    /**
     * 화면의 모든 글에 적용한다. 레이아웃에 적은 글뿐 아니라 코드에서 나중에 넣은 글도
     * 그리기 직전에 찾아 고치므로, 글을 넣는 곳마다 따로 챙기지 않아도 된다 (BaseActivity에서 부른다)
     */
    public static void watch(View root) {
        if (Build.VERSION.SDK_INT >= 33) return;
        // 고친 글이 있으면 이번 그리기를 건너뛰고 바뀐 줄바꿈으로 다시 그린다
        root.getViewTreeObserver().addOnPreDrawListener(() -> !applyTo(root));
    }

    /** 고친 글이 있으면 true */
    private static boolean applyTo(View v) {
        boolean changed = false;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) changed |= applyTo(g.getChildAt(i));
        } else if (v instanceof TextView && !(v instanceof EditText)) {
            TextView t = (TextView) v;
            // 복사할 수 있는 원문 글은 그대로 둔다 (복사한 글에 보이지 않는 글자가 섞이지 않게)
            if (!t.isTextSelectable() && needsJoin(t.getText())) {
                t.setText(join(t.getText()));
                changed = true;
            }
        }
        return changed;
    }

    static boolean needsJoin(CharSequence s) {
        for (int i = 1; i < s.length(); i++) if (joinable(s, i)) return true;
        return false;
    }

    /** 꾸밈(색, 크기)이 들어간 글은 꾸밈을 지킨 채 넣는다 */
    static CharSequence join(CharSequence s) {
        if (!(s instanceof Spanned)) return join(s.toString());
        SpannableStringBuilder b = new SpannableStringBuilder(s);
        for (int i = b.length() - 1; i > 0; i--) if (joinable(b, i)) b.insert(i, String.valueOf(WORD_JOINER));
        return b;
    }

    static String join(String s) {
        StringBuilder b = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && joinable(s, i)) b.append(WORD_JOINER);
            b.append(s.charAt(i));
        }
        return b.toString();
    }

    /** i번째 글자 앞에 넣어야 하는지: 같은 낱말 안이고, 이미 넣지 않았고, 이모지 같은 두 칸 글자의 가운데가 아닐 때 */
    private static boolean joinable(CharSequence s, int i) {
        char c = s.charAt(i), p = s.charAt(i - 1);
        return !Character.isWhitespace(c) && !Character.isWhitespace(p)
                && c != WORD_JOINER && p != WORD_JOINER && !Character.isLowSurrogate(c);
    }
}
