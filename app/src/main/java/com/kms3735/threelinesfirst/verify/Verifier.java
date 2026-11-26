package com.kms3735.threelinesfirst.verify;

import com.kms3735.threelinesfirst.ai.Summary;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 요약에 나온 날짜와 금액이 원문에 실제로 있는지 코드로 대조한다.
 * 글자 인식 결과의 흔한 오류(월을 "원"으로 읽음, 숫자 사이 공백, 표 금액의 "원" 빠짐)를 견딘다.
 */
public final class Verifier {

    public static final class Result {
        /** 원문에서 찾지 못한 값. "11월 30일", "45,000원"처럼 화면에 그대로 보여 줄 수 있는 글 */
        public final List<String> mismatches;
        /** 대조한 날짜·금액 개수. 0이면 대조할 것이 없었다 */
        public final int checked;

        Result(List<String> mismatches, int checked) {
            this.mismatches = mismatches;
            this.checked = checked;
        }

        public boolean passed() {
            return mismatches.isEmpty();
        }
    }

    // 2026. 11. 25. / 2026.11.20 / 2026-11-25 / 2026년 11월 25일 (월을 "원"으로 읽은 경우 포함)
    private static final Pattern FULL_DATE = Pattern.compile(
            "(?<!\\d)(\\d{4})\\s*[.\\-/년]\\s*(\\d{1,2})\\s*[.\\-/월원]\\s*(\\d{1,2})(?!\\d)");
    // 11월 25일 (연도 없음)
    private static final Pattern MONTH_DAY = Pattern.compile(
            "(?<![\\d.])(\\d{1,2})\\s*[월원]\\s*(\\d{1,2})\\s*일");

    // 15만 2천원, 3만원
    private static final Pattern MAN_WON = Pattern.compile("(?<![\\d,])(\\d+)\\s*만(?:\\s*(\\d+)\\s*천)?\\s*원");
    // 5천원
    private static final Pattern CHEON_WON = Pattern.compile("(?<![\\d,])(\\d+)\\s*천\\s*원");
    // 152,000 (표 안 금액은 "원"이 빠지기도 하고, 쉼표 앞뒤에 공백이 끼기도 한다)
    private static final Pattern COMMA_NUMBER = Pattern.compile("(?<![\\d,])\\d{1,3}(?:\\s?,\\s?\\d{3})+(?!\\d)");
    // 152000원, 0원
    private static final Pattern PLAIN_WON = Pattern.compile("(?<![\\d,])(\\d+)\\s*원");

    // 이 말이 있는 줄의 날짜를 마감일 후보로 본다. "기간"은 부과 기간처럼 마감일이 아닌 경우가 많아 뺀다
    private static final Pattern DEADLINE_LINE = Pattern.compile("까지|마감|기한|납기");

    private Verifier() {
    }

    /**
     * AI가 옮긴 전화번호의 숫자가 원문에 그대로 있으면 그 번호를, 없으면 빈 문자열을 돌려준다.
     * 글자 인식이 숫자 사이에 공백을 넣어도(02-1 23-4567) 숫자만 비교하므로 찾는다
     */
    public static String phoneIn(String original, String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 8) return "";
        return original.replaceAll("\\D", "").contains(digits) ? phone.trim() : "";
    }

    public static Result check(String original, Summary s, LocalDate today) {
        Dates origin = new Dates(original);
        Set<Long> originAmounts = amounts(original);
        Set<String> mismatches = new LinkedHashSet<>();
        int checked = 0;

        // 1) 마감일 칸: 원문에 있어야 하고, 원문에 마감일 후보가 있으면 그중 하나여야 한다
        if (!s.deadline.isEmpty()) {
            checked++;
            LocalDate d = LocalDate.parse(s.deadline);
            Dates candidates = new Dates(deadlineLines(original));
            boolean ok = origin.contains(d.getYear(), d.getMonthValue(), d.getDayOfMonth())
                    && (candidates.isEmpty() || candidates.contains(d.getYear(), d.getMonthValue(), d.getDayOfMonth()));
            // 원문에 연도가 없으면 연도는 오늘 기준으로 정한다 (지난 날짜면 다음 해)
            if (ok && !origin.containsFull(d)) ok = d.getYear() == inferYear(d.getMonthValue(), d.getDayOfMonth(), today);
            if (!ok) mismatches.add(dateLabel(d.getMonthValue(), d.getDayOfMonth()));
        }

        // 2) 금액 칸
        for (Long amount : s.amounts) {
            if (amount == null || amount == 0) continue; // "무료(0원)"는 대조하지 않는다
            checked++;
            if (!originAmounts.contains(amount)) mismatches.add(wonLabel(amount));
        }

        // 3) 사용자가 실제로 읽는 문장 속 날짜와 금액
        String shown = s.line1What + "\n" + s.line2Todo + "\n" + s.line3WhenWhere + "\n" + s.warning
                + "\n" + String.join("\n", s.todos);
        for (int[] d : new Dates(shown).all) {
            checked++;
            if (!origin.contains(d[0], d[1], d[2])) mismatches.add(dateLabel(d[1], d[2]));
        }
        for (long amount : amounts(shown)) {
            if (amount == 0) continue;
            checked++;
            if (!originAmounts.contains(amount)) mismatches.add(wonLabel(amount));
        }
        return new Result(new ArrayList<>(mismatches), checked);
    }

    /** 글 속 금액을 원 단위로. 겹치는 표기는 큰 단위부터 찾고 찾은 자리는 지운다 */
    static Set<Long> amounts(String text) {
        Set<Long> out = new HashSet<>();
        StringBuilder rest = new StringBuilder(text);
        take(rest, MAN_WON, m -> out.add(Long.parseLong(m.group(1)) * 10_000
                + (m.group(2) == null ? 0 : Long.parseLong(m.group(2)) * 1_000)));
        take(rest, CHEON_WON, m -> out.add(Long.parseLong(m.group(1)) * 1_000));
        take(rest, COMMA_NUMBER, m -> out.add(Long.parseLong(m.group().replaceAll("[^0-9]", ""))));
        take(rest, PLAIN_WON, m -> out.add(Long.parseLong(m.group(1))));
        return out;
    }

    /** 글 속 날짜 모음. 연도가 없는 날짜는 year = 0 */
    static final class Dates {
        final List<int[]> all = new ArrayList<>();

        Dates(String text) {
            StringBuilder rest = new StringBuilder(text);
            take(rest, FULL_DATE, m -> add(Integer.parseInt(m.group(1)), m.group(2), m.group(3)));
            take(rest, MONTH_DAY, m -> add(0, m.group(1), m.group(2)));
        }

        private void add(int year, String month, String day) {
            int mo = Integer.parseInt(month), d = Integer.parseInt(day);
            try {
                LocalDate.of(year == 0 ? 2000 : year, mo, d); // 2000년은 윤년이라 2월 29일도 통과
                all.add(new int[]{year, mo, d});
            } catch (DateTimeException ignored) {
                // 13월, 32일처럼 날짜가 아닌 숫자
            }
        }

        boolean isEmpty() {
            return all.isEmpty();
        }

        boolean containsFull(LocalDate d) {
            for (int[] x : all) {
                if (x[0] == d.getYear() && x[1] == d.getMonthValue() && x[2] == d.getDayOfMonth()) return true;
            }
            return false;
        }

        /**
         * 같은 날짜가 있는지. 연도가 있으면 연도까지 같아야 하고,
         * 한쪽에 연도가 없으면 월·일만 비교한다
         */
        boolean contains(int year, int month, int day) {
            for (int[] x : all) {
                if (x[1] != month || x[2] != day) continue;
                if (year == 0 || x[0] == 0 || x[0] == year) return true;
            }
            return false;
        }
    }

    private static String deadlineLines(String text) {
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n")) {
            if (DEADLINE_LINE.matcher(line).find()) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    static int inferYear(int month, int day, LocalDate today) {
        LocalDate thisYear = LocalDate.of(today.getYear(), month, day == 29 && month == 2 ? 28 : day);
        return thisYear.isBefore(today) ? today.getYear() + 1 : today.getYear();
    }

    private interface OnMatch {
        void accept(Matcher m);
    }

    /** 찾은 곳을 공백으로 지워, 뒤의 패턴이 같은 글자를 다시 잡지 않게 한다 */
    private static void take(StringBuilder rest, Pattern p, OnMatch onMatch) {
        Matcher m = p.matcher(rest.toString());
        while (m.find()) {
            onMatch.accept(m);
            for (int i = m.start(); i < m.end(); i++) rest.setCharAt(i, ' ');
        }
    }

    private static String dateLabel(int month, int day) {
        return month + "월 " + day + "일";
    }

    private static String wonLabel(long amount) {
        return String.format(Locale.KOREA, "%,d원", amount);
    }
}
