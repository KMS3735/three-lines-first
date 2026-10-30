package com.kms3735.threelinesfirst.privacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** 테스트용 가상 안내문 5종을 에뮬레이터에서 글자 인식한 실제 결과로 검증한다. 모든 개인정보는 가짜다. */
public class PrivacyMaskerTest {

    private static void assertMasked(String masked, String... secrets) {
        for (String secret : secrets) {
            assertFalse("남아 있음: " + secret, masked.contains(secret));
        }
    }

    private static void assertKept(String masked, String... keep) {
        for (String k : keep) {
            assertTrue("지워짐: " + k, masked.contains(k));
        }
    }

    @Test
    public void 관리비_고지서() {
        String ocr = "2026년 10월분 관리비 납부고지서\n"
                + "가나 행복마을 아파트 관리사무소\n"
                + "동호수  101동 1001호\n"
                + "입주자  홍길동\n"
                + "납부 마감일  2026. 11. 25.\n"
                + "이번 달 납부액  152,000\n"
                + "납부 계좌: 가나은행 123-456789-01-234 (예금주: 행복마을관리사무소)\n"
                + "문의: 관리사무소 02-1 23-4567 (평일 09:00 ~ 18:00 )";
        String m = PrivacyMasker.mask(ocr).text;
        assertMasked(m, "홍길동", "123-456789-01-234");
        assertKept(m, "2026. 11. 25.", "152,000", "02-1 23-4567", "행복마을관리사무소", "[이름]", "[계좌번호]");
    }

    @Test
    public void 건강검진_안내문() {
        String ocr = "2026년 일반건강검진 대상자 안내\n"
                + "국민건강보험공단 가나지사\n"
                + "홍길순 남께\n"
                + "주민등록번호: 540315-2345678\n"
                + "주소: 서울특별시 가나구 다라로 12, 3층\n"
                + "[검진 기간]  [준비물]\n"
                + "2026. 1. 1. ~ 2026. 12. 31.  신분증, 이 안내문\n"
                + "검진기관 찾기: 건강검진 지정기관 안내 1588-0000\n"
                + "보호자 연락처: 010-1234-5678";
        PrivacyMasker.Result r = PrivacyMasker.mask(ocr);
        assertMasked(r.text, "홍길순", "540315-2345678", "다라로", "010-1234-5678");
        assertKept(r.text, "대상자 안내", "2026. 12. 31.", "1588-0000", "[주민번호]", "[주소]", "[휴대전화]");
        assertEquals(4, r.count);
    }

    @Test
    public void 자동차세_고지서_숫자_사이_공백() {
        String ocr = "납세자  김철수\n"
                + "납부 기한  2026. 12. 31.\n"
                + "납부할 세액 함계  169,000\n"
                + "가상계좌: 다마은행 987-654-321 098 (납세자 전용)\n"
                + "위택스(www.wetax.g o.kr) 또는 가까운 은행에서 납부할 수 있습니다.\n"
                + "문의: 가나시 세무과 031-000-1234";
        String m = PrivacyMasker.mask(ocr).text;
        assertMasked(m, "김철수", "987-654-321 098", "321 098");
        assertKept(m, "(납세자 전용)", "169,000", "031-000-1234", "가까운 은행에서");
    }

    @Test
    public void 과태료_사전통지서() {
        String ocr = "성명  이영희\n"
                + "위반 일사  2026. 10. 12. 14:32\n"
                + "위반 장소  가나구 행복로 45 잎\n"
                + "과태료 금액  40,000l\n"
                + "기한 안에 자진 납부하면 20% 감경된 32,000원만 내면 됩니다.\n"
                + "납부 계조좌: 가나은행 555-66-777888 (가나구청)\n"
                + "문의: 교통행정과 02-987-6543";
        String m = PrivacyMasker.mask(ocr).text;
        assertMasked(m, "이영희", "555-66-777888");
        assertKept(m, "가나구 행복로 45", "32,000원", "02-987-6543", "2026. 10. 12.");
    }

    @Test
    public void 기초연금_신청안내() {
        String ocr = "박순자 어르신께\n"
                + "신청 기간  2026. 11. 1. ~ 2026. 11. 28.\n"
                + "신청 장소  주소지 행정복지센터 또는 국민연금공단 지사\n"
                + "지급 예정액  월 최대 342,510원\n"
                + "연금 받을 계좌: 행복은행 110-222-333444 (본인 명의만 가능)\n"
                + "문의: 행복동 행정복지센터 복지팀 02-555-01 29";
        String m = PrivacyMasker.mask(ocr).text;
        assertMasked(m, "박순자", "110-222-333444");
        assertKept(m, "[이름] 어르신께", "342,510원", "02-555-01 29", "주소지 행정복지센터");
    }

    @Test
    public void 계좌번호_중간에_잘못_읽힌_글자가_끼어도_가린다() {
        // 실제 카드 명세서 인식에서 나온 형태 (숫자는 가짜)
        String m = PrivacyMasker.mask("-결제계좌 농협 12345야67\n포인트 및 마일리지").text;
        assertMasked(m, "12345야67");
        assertKept(m, "[계좌번호]", "포인트 및 마일리지");
    }

    @Test
    public void 띄어쓴_주민번호와_휴대전화도_가린다() {
        String m = PrivacyMasker.mask("주민번호 540315 - 2345678, 연락처 010 1234 5678").text;
        assertMasked(m, "2345678", "5678");
        assertKept(m, "[주민번호]", "[휴대전화]");
    }

    @Test
    public void 문자_메시지_중간의_이름도_가린다() {
        String m = PrivacyMasker.mask("[관리비] 홍길동 님 이번 달 152,000원, 김영수님께 안내드립니다. 고객님 문의는 1577-1000").text;
        assertMasked(m, "홍길동", "김영수");
        assertKept(m, "[이름] 님 이번 달", "[이름]님께", "고객님", "152,000원", "1577-1000");
    }

    @Test
    public void 개인정보가_없으면_그대로() {
        String ocr = "[국민건강보험] 2026년 건강검진 대상입니다. 12월 31일까지 지정 병원에서 받으세요. 문의 1577-1000";
        PrivacyMasker.Result r = PrivacyMasker.mask(ocr);
        assertEquals(ocr, r.text);
        assertEquals(0, r.count);
    }
}
