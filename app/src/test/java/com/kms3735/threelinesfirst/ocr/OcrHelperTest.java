package com.kms3735.threelinesfirst.ocr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OcrHelperTest {

    @Test
    public void 공백은_글자_수에서_뺀다() {
        assertFalse(OcrHelper.isEnough("관리비\n\n   고지서        \t\t\t                    "));
    }

    @Test
    public void 서른_글자_이상이면_통과() {
        assertTrue(OcrHelper.isEnough("아파트 관리비 고지서 10월 31일까지 152,000원을 은행에서 납부"));
    }

    @Test
    public void 빈_문자열은_실패() {
        assertFalse(OcrHelper.isEnough(""));
    }
}
