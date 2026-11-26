package com.kms3735.threelinesfirst.ai;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** AI가 돌려준 세 줄 요약. 5단계에서 Room의 Notice·Todo로 옮겨 저장한다 */
public class Summary {
    @SerializedName("line1_what") public String line1What;
    @SerializedName("line2_todo") public String line2Todo;
    @SerializedName("line3_when_where") public String line3WhenWhere;
    public String warning;
    public List<String> todos;
    /** YYYY-MM-DD, 마감일이 없으면 빈 문자열 */
    public String deadline;
    /** 원 단위 금액 */
    public List<Long> amounts;
    /** 문의할 기관 전화번호. 없으면 빈 문자열 */
    public String phone;
    /** 직접 찾아가야 하는 기관 이름 (지도 검색용). 없으면 빈 문자열 */
    public String place;

    /** 형식 검사: 세 줄은 비어 있으면 안 되고, 나머지 칸도 빠지면 안 된다 */
    boolean isValid() {
        return !isBlank(line1What) && !isBlank(line2Todo) && !isBlank(line3WhenWhere)
                && warning != null && todos != null && amounts != null
                && deadline != null && (deadline.isEmpty() || deadline.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
