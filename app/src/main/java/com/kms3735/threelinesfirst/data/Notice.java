package com.kms3735.threelinesfirst.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.kms3735.threelinesfirst.ai.Summary;
import com.kms3735.threelinesfirst.verify.Verifier;

/**
 * 안내문 한 건. 요약은 안내문마다 하나뿐이라 같은 표에 둔다.
 * 요약 전에 먼저 저장해 두므로, 요약에 실패해도 목록에서 다시 시도할 수 있다.
 */
@Entity(tableName = "notice")
public class Notice {
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 개인정보를 가린 글. 가리기 전 원문은 저장하지 않는다 */
    public String maskedText;
    /** 앱 내부 저장소의 원문 사진 경로. 공유받은 글은 null */
    public String imagePath;
    public long createdAt;

    // 세 줄 요약. line1What이 null이면 아직 요약 전
    public String line1What;
    public String line2Todo;
    public String line3WhenWhere;
    public String warning;
    /** YYYY-MM-DD, 없으면 빈 문자열 */
    public String deadline;
    /** 원 단위 금액을 쉼표로 이음. 예: "40000,32000" */
    public String amounts;

    // 원문 대조 결과
    /** 대조한 날짜·금액 개수. 0이면 배지를 숨긴다 */
    public int checkedCount;
    /** 원문과 다른 값을 ", "로 이음. 통과면 빈 문자열 */
    public String mismatches;

    /** 문의 전화. 원문에 있는 번호만 남긴다 */
    public String phone;
    /** 지도에서 찾을 기관 이름 */
    public String place;

    public boolean isSummarized() {
        return line1What != null;
    }

    public void apply(Summary s, Verifier.Result r) {
        line1What = s.line1What;
        line2Todo = s.line2Todo;
        line3WhenWhere = s.line3WhenWhere;
        warning = s.warning;
        deadline = s.deadline;
        StringBuilder sb = new StringBuilder();
        for (Long a : s.amounts) {
            if (a == null) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(a);
        }
        amounts = sb.toString();
        checkedCount = r.checked;
        mismatches = String.join(", ", r.mismatches);
        phone = Verifier.phoneIn(maskedText == null ? "" : maskedText, s.phone);
        place = s.place == null ? "" : s.place.trim();
    }
}
