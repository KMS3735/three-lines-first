package com.kms3735.threelinesfirst.privacy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI로 보내기 전에 기기 안에서 개인정보를 가린다.
 * 글자 인식 결과에는 숫자 사이 공백("987-654-321 098")이나 잘못 끼어든 글자("17746야18")가
 * 섞이므로, 깔끔한 형식만 찾지 않고 이런 형태도 함께 잡는다.
 * 기관 대표번호(02-, 031-, 1588-)는 전화 걸기에 쓰므로 가리지 않는다.
 */
public final class PrivacyMasker {

    public static final class Result {
        public final String text;
        public final int count;

        Result(String text, int count) {
            this.text = text;
            this.count = count;
        }
    }

    static final String RRN = "[주민번호]";
    static final String MOBILE = "[휴대전화]";
    static final String ACCOUNT = "[계좌번호]";
    static final String NAME = "[이름]";
    static final String ADDRESS = "[주소]";

    // 주민등록번호: 앞 6자리 - 뒤 7자리, 숫자 사이 공백 허용
    private static final Pattern RRN_P = Pattern.compile(
            "(?<!\\d)\\d(?:\\s?\\d){5}\\s*-\\s*[1-8](?:\\s?\\d){6}(?!\\d)");

    // 개인 휴대전화: 010 등으로 시작, 하이픈·공백 허용
    private static final Pattern MOBILE_P = Pattern.compile(
            "(?<!\\d)01[016789](?:[\\s-]*\\d){7,8}(?!\\d)");

    // 계좌번호 앞에 오는 말. 은행 이름은 대부분 "○○은행"으로 끝난다
    private static final Pattern ACCOUNT_KEY_P = Pattern.compile(
            "계좌|통장|은행|농협|신협|우체국|새마을금고|카카오뱅크|토스뱅크|케이뱅크");

    // 표의 항목 이름 뒤 이름. 이름 뒤가 줄 끝, 표 칸 구분(공백 두 칸), "님"일 때만 이름으로 본다
    private static final Pattern NAME_LABEL_P = Pattern.compile(
            "(성명|이름|입주자|납세자|수검자|수급자|신청인|예금주)\\s*[:：]?\\s*([가-힣]{2,4})(?=\\s*(?:\\n|$)|  |\\s*님)");

    // 줄 맨 앞의 "홍길동 님께", "박순자 어르신께" (글자 인식이 "님"을 "남"으로 읽는 경우 포함)
    private static final Pattern NAME_GREETING_P = Pattern.compile(
            "(?m)^(\\s*)([가-힣]{2,4})(\\s*(?:님께|남께|님|어르신께|어르신|귀하))");

    // 문장 중간의 "홍길동 님", "홍길동님께" (문자·카카오톡 안내문에 흔함)
    private static final Pattern NAME_HONORIFIC_P = Pattern.compile(
            "(?<![가-힣])([가-힣]{2,4})(\\s?(?:님께|남께|님))");

    // "님"이 붙어도 사람 이름이 아닌 말
    private static final Set<String> NOT_NAMES = new HashSet<>(Arrays.asList(
            "고객", "회원", "어르신", "선생", "학생", "환자", "보호자", "담당자", "주민", "가입자",
            "납세자", "대상자", "신청인", "입주자", "사용자", "운전자", "소유자", "부모", "여러분"));

    // "주소:" 또는 표의 "주소  ..." 뒤의 나머지 줄. "주소지 행정복지센터" 같은 안내 문장은 건드리지 않는다
    private static final Pattern ADDRESS_LABEL_P = Pattern.compile("(주소\\s*[:：]\\s*|주소  )([^\\n]+)");

    // "서울특별시 가나구 다라로 12" 같은 도로명 주소
    private static final Pattern ADDRESS_ROAD_P = Pattern.compile(
            "[가-힣]+(?:특별시|광역시|특별자치시|특별자치도|도|시)\\s+[가-힣]+(?:구|군|시)\\s+[가-힣0-9]+(?:로|길)\\s*\\d+[^\\n]*");

    private PrivacyMasker() {
    }

    public static Result mask(String text) {
        int[] count = {0};
        String s = text;
        s = replaceAll(s, RRN_P, m -> RRN, count);
        s = replaceAll(s, MOBILE_P, m -> MOBILE, count);
        s = maskAccounts(s, count);
        s = replaceAll(s, ADDRESS_LABEL_P, m -> m.group(1) + ADDRESS, count);
        s = replaceAll(s, ADDRESS_ROAD_P, m -> ADDRESS, count);
        s = replaceAll(s, NAME_LABEL_P, m -> m.group(0).substring(0, m.start(2) - m.start()) + NAME, count);
        s = replaceAll(s, NAME_GREETING_P, m -> m.group(1) + NAME + m.group(3), count);
        s = replaceAll(s, NAME_HONORIFIC_P,
                m -> NOT_NAMES.contains(m.group(1)) ? null : NAME + m.group(2), count);
        return new Result(s, count[0]);
    }

    /** 바꿀 글을 돌려준다. null이면 바꾸지 않는다 */
    private interface Replacer {
        String apply(Matcher m);
    }

    private static String replaceAll(String s, Pattern p, Replacer r, int[] count) {
        Matcher m = p.matcher(s);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            String replacement = r.apply(m);
            if (replacement == null) continue;
            sb.append(s, last, m.start()).append(replacement);
            last = m.end();
            count[0]++;
        }
        return sb.append(s.substring(last)).toString();
    }

    /**
     * 계좌 관련 말 뒤, 같은 줄 40글자 안의 숫자 덩어리를 가린다.
     * 숫자·하이픈·공백으로 이어지고, 중간에 잘못 읽힌 한글이 한 글자 끼어 있어도 하나로 본다.
     */
    private static String maskAccounts(String s, int[] count) {
        StringBuilder sb = new StringBuilder(s);
        Matcher key = ACCOUNT_KEY_P.matcher(s);
        int searchFrom = 0;
        // 뒤에서부터 바꾸면 앞쪽 위치가 흐트러지지 않는다
        java.util.List<int[]> ranges = new java.util.ArrayList<>();
        while (key.find(searchFrom)) {
            int lineEnd = s.indexOf('\n', key.end());
            if (lineEnd < 0) lineEnd = s.length();
            int[] run = findNumberRun(s, key.end(), Math.min(lineEnd, key.end() + 40));
            if (run != null) {
                ranges.add(run);
                searchFrom = run[1];
            } else {
                searchFrom = key.end();
            }
            if (searchFrom >= s.length()) break;
        }
        for (int i = ranges.size() - 1; i >= 0; i--) {
            sb.replace(ranges.get(i)[0], ranges.get(i)[1], ACCOUNT);
            count[0]++;
        }
        return sb.toString();
    }

    private static int[] findNumberRun(String s, int from, int to) {
        for (int start = from; start < to; start++) {
            if (!Character.isDigit(s.charAt(start))) continue;
            int digits = 0, hangul = 0, end = start;
            for (int i = start; i < to; i++) {
                char c = s.charAt(i);
                if (Character.isDigit(c)) {
                    digits++;
                    end = i + 1;
                } else if (c == '-' || c == ' ') {
                    // 구분 기호는 숫자가 다시 이어질 때만 포함
                } else if (isHangul(c) && hangul == 0 && i + 1 < to && Character.isDigit(s.charAt(i + 1))) {
                    hangul++;
                } else {
                    break;
                }
            }
            if (digits >= 6) return new int[]{start, end};
            start = end; // 짧은 숫자(금액, 날짜 조각)는 건너뛴다
        }
        return null;
    }

    private static boolean isHangul(char c) {
        return c >= '가' && c <= '힣';
    }
}
