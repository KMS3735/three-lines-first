package com.kms3735.threelinesfirst.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.kms3735.threelinesfirst.ai.Summary;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 테스트용 가상 안내문 5종을 에뮬레이터에서 인식한 실제 결과(test/resources/ocr)로 검증한다.
 * 정답 마감일·금액은 안내문을 만들 때 함께 만든 answers.json 값이다.
 */
public class VerifierTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    private static String ocr(int n) throws IOException {
        try (InputStream in = VerifierTest.class.getResourceAsStream("/ocr/sample0" + n + ".txt")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Summary summary(String line3, String deadline, Long... amounts) {
        Summary s = new Summary();
        s.line1What = "안내문이에요.";
        s.line2Todo = "해야 할 일이 있어요.";
        s.line3WhenWhere = line3;
        s.warning = "";
        s.todos = new ArrayList<>();
        s.deadline = deadline;
        s.amounts = new ArrayList<>(Arrays.asList(amounts));
        return s;
    }

    private static List<String> mismatches(String original, Summary s) {
        return Verifier.check(original, s, TODAY).mismatches;
    }

    @Test
    public void 정답_요약은_5종_모두_통과() throws IOException {
        Summary[] answers = {
                summary("2026년 11월 25일까지 가나은행 계좌로 152,000원을 내세요.", "2026-11-25", 152000L),
                summary("2026년 12월 31일까지 가까운 지정 검진기관에서 무료로 받아요.", "2026-12-31", 0L),
                summary("2026년 12월 31일까지 은행이나 위택스에서 169,000원을 내세요.", "2026-12-31", 130000L, 39000L, 169000L),
                summary("2026년 11월 20일까지 가나은행 계좌로 32,000원을 내세요.", "2026-11-20", 40000L, 32000L),
                summary("11월 28일까지 행정복지센터에서 신청하면 월 최대 342,510원을 받아요.", "2026-11-28", 342510L),
        };
        for (int i = 0; i < answers.length; i++) {
            Verifier.Result r = Verifier.check(ocr(i + 1), answers[i], TODAY);
            assertTrue("샘플 " + (i + 1) + ": " + r.mismatches, r.passed());
            assertTrue(r.checked > 0);
        }
    }

    @Test
    public void 틀린_마감일은_잡는다() throws IOException {
        List<String> m = mismatches(ocr(1), summary("2026년 11월 25일까지 내세요.", "2026-11-30", 152000L));
        assertEquals(Arrays.asList("11월 30일"), m);
    }

    @Test
    public void 문장_속_틀린_날짜도_잡는다() throws IOException {
        // 마감일 칸은 맞아도 사용자가 읽는 문장이 틀리면 안 된다
        List<String> m = mismatches(ocr(1), summary("11월 30일까지 152,000원을 내세요.", "2026-11-25", 152000L));
        assertEquals(Arrays.asList("11월 30일"), m);
    }

    @Test
    public void 원문에_있어도_마감일이_아닌_날짜는_잡는다() throws IOException {
        // 과태료 통지서의 위반 일시(10월 12일)를 마감일로 고른 경우
        List<String> m = mismatches(ocr(4), summary("가나은행 계좌로 32,000원을 내세요.", "2026-10-12", 32000L));
        assertEquals(Arrays.asList("10월 12일"), m);
        // 관리비 고지서의 부과 기간 끝날(10월 31일)도 마감일이 아니다
        assertEquals(Arrays.asList("10월 31일"),
                mismatches(ocr(1), summary("관리비를 내세요.", "2026-10-31", 152000L)));
    }

    @Test
    public void 틀린_금액은_잡는다() throws IOException {
        List<String> m = mismatches(ocr(4), summary("11월 20일까지 45,000원을 내세요.", "2026-11-20", 45000L));
        assertEquals(Arrays.asList("45,000원"), m);
    }

    @Test
    public void 금액_표기가_달라도_같은_값으로_본다() {
        assertEquals(new java.util.HashSet<>(Arrays.asList(152000L)), Verifier.amounts("15만 2천원"));
        assertEquals(new java.util.HashSet<>(Arrays.asList(30000L)), Verifier.amounts("3만원"));
        assertEquals(new java.util.HashSet<>(Arrays.asList(152000L)), Verifier.amounts("납부액  152, 000"));   // 쉼표 옆 공백
        assertEquals(new java.util.HashSet<>(Arrays.asList(40000L)), Verifier.amounts("과태료 금액  40,000l")); // "원"을 l로 읽음
        assertTrue(mismatches("이번 달 152,000원", summary("15만 2천원을 내세요.", "")).isEmpty());
    }

    @Test
    public void 월을_원으로_잘못_읽은_날짜도_찾는다() {
        assertTrue(mismatches("납부 마감 12원 31일까지", summary("12월 31일까지 내세요.", "2026-12-31")).isEmpty());
        assertTrue(mismatches("납부 마감일 2026년 10원 31일", summary("10월 31일까지 내세요.", "2026-10-31")).isEmpty());
    }

    @Test
    public void 연도_없는_문자_안내는_오늘_기준으로_연도를_정한다() {
        String sms = "[가나구청] [이름]님, 2기분 주민세 52,000원이 부과되었습니다. 납부기한 12월 15일.";
        assertTrue(mismatches(sms, summary("12월 15일까지 52,000원을 내세요.", "2026-12-15", 52000L)).isEmpty());
        assertEquals(Arrays.asList("12월 15일"), mismatches(sms, summary("12월 15일까지 내세요.", "2025-12-15")));
        // 이미 지난 날짜면 다음 해
        assertEquals(2027, Verifier.inferYear(1, 10, TODAY));
        assertEquals(2026, Verifier.inferYear(12, 15, TODAY));
    }

    @Test
    public void 대조할_값이_없으면_checked는_0() {
        Verifier.Result r = Verifier.check("안내문", summary("행정복지센터에서 신청해요.", ""), TODAY);
        assertTrue(r.passed());
        assertEquals(0, r.checked);
        assertFalse(Verifier.check("안내문", summary("5천원을 내요.", ""), TODAY).passed());
    }
}
