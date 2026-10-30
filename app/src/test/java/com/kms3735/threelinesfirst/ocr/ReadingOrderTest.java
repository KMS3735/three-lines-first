package com.kms3735.threelinesfirst.ocr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;

public class ReadingOrderTest {

    private static ReadingOrder.Line line(String text, int left, int top) {
        return new ReadingOrder.Line(text, left, top, left + 200, top + 40);
    }

    @Test
    public void 표의_항목과_내용을_같은_줄로_잇는다() {
        // ML Kit이 왼쪽 칸을 먼저, 오른쪽 칸을 나중에 준 경우
        String text = ReadingOrder.arrange(Arrays.asList(
                line("납부기한", 0, 100),
                line("납부금액", 0, 160),
                line("2026. 10. 31.", 400, 104),
                line("152,000원", 400, 158)));
        assertEquals("납부기한  2026. 10. 31.\n납부금액  152,000원", text);
    }

    @Test
    public void 한_칸짜리_문서는_위에서_아래로() {
        String text = ReadingOrder.arrange(Arrays.asList(
                line("둘째 줄", 0, 160),
                line("첫째 줄", 0, 100)));
        assertEquals("첫째 줄\n둘째 줄", text);
    }

    @Test
    public void 기울어진_사진은_각도를_바로_세워_좌우_칸을_맞춘다() {
        // 반시계로 4도 기운 사진: 오른쪽 칸(x=700)이 같은 줄인데도 약 49px 위에 찍힌다
        float a = -4f;
        String text = ReadingOrder.arrange(Arrays.asList(
                new ReadingOrder.Line("[검진 기간]", 0, 300, 200, 340, a),
                new ReadingOrder.Line("2026. 12. 31.", 0, 360, 200, 400, a),
                new ReadingOrder.Line("[준비물]", 600, 251, 800, 291, a),
                new ReadingOrder.Line("신분증", 600, 311, 800, 351, a)));
        assertEquals("[검진 기간]  [준비물]\n2026. 12. 31.  신분증", text);
    }

    @Test
    public void 조금_기울어진_줄도_같은_행으로_본다() {
        String text = ReadingOrder.arrange(Arrays.asList(
                line("오른쪽", 400, 112),
                line("왼쪽", 0, 100)));
        assertEquals("왼쪽  오른쪽", text);
    }
}
