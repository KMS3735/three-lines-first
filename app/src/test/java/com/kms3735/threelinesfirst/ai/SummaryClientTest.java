package com.kms3735.threelinesfirst.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

/** Gemini 응답의 형식 검사. 형식이 틀리면 null을 돌려 한 번 더 요청하게 한다 */
public class SummaryClientTest {

    private static final String GOOD = "{\"line1_what\":\"아파트 관리비 고지서예요.\","
            + "\"line2_todo\":\"이번 달 관리비를 내셔야 해요.\","
            + "\"line3_when_where\":\"11월 25일까지 152,000원을 은행에서 내요.\","
            + "\"warning\":\"기한이 지나면 연체료가 붙어요.\","
            + "\"todos\":[\"관리비 내기\",\"\"],\"deadline\":\"2026-11-25\",\"amounts\":[152000]}";

    /** 실제 generateContent 응답과 같은 모양으로 감싼다 */
    private static JsonObject response(String text) {
        return JsonParser.parseString("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + new com.google.gson.JsonPrimitive(text) + "}]}}]}").getAsJsonObject();
    }

    @Test
    public void 올바른_응답은_요약으로_바꾼다() {
        Summary s = SummaryClient.parse(response(GOOD));
        assertNotNull(s);
        assertEquals("아파트 관리비 고지서예요.", s.line1What);
        assertEquals("2026-11-25", s.deadline);
        assertEquals(Collections.singletonList(152000L), s.amounts);
        assertEquals(Collections.singletonList("관리비 내기"), s.todos); // 빈 할 일은 뺀다
    }

    @Test
    public void 마감일이_없으면_빈_문자열도_통과() {
        assertNotNull(SummaryClient.parse(response(GOOD.replace("2026-11-25", ""))));
    }

    @Test
    public void 형식이_틀리면_null() {
        assertNull(SummaryClient.parse(response(GOOD.replace("아파트 관리비 고지서예요.", " "))));   // 빈 줄
        assertNull(SummaryClient.parse(response(GOOD.replace("\"warning\":\"기한이 지나면 연체료가 붙어요.\",", "")))); // 칸 빠짐
        assertNull(SummaryClient.parse(response(GOOD.replace("2026-11-25", "11월 25일"))));     // 날짜 형식
        assertNull(SummaryClient.parse(response(GOOD.replace("[152000]", "[\"십오만\"]"))));     // 금액이 숫자가 아님
        assertNull(SummaryClient.parse(response(GOOD.substring(0, 40))));                      // 잘린 JSON
        assertNull(SummaryClient.parse(JsonParser.parseString("{\"candidates\":[]}").getAsJsonObject()));
        assertNull(SummaryClient.parse(JsonParser.parseString("{}").getAsJsonObject()));
    }

    @Test
    public void 요청에_가린_글과_오늘_날짜와_JSON_형식이_들어간다() {
        JsonObject body = SummaryClient.buildRequest("[이름] 님 관리비 안내", LocalDate.of(2026, 11, 10));
        String userText = body.getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();
        assertTrue(userText.contains("2026-11-10"));
        assertTrue(userText.contains("[이름] 님 관리비 안내"));

        JsonObject config = body.getAsJsonObject("generationConfig");
        assertEquals("application/json", config.get("responseMimeType").getAsString());
        JsonArray required = config.getAsJsonObject("responseSchema").getAsJsonArray("required");
        for (String field : Arrays.asList("line1_what", "line2_todo", "line3_when_where", "warning", "todos", "deadline", "amounts")) {
            assertTrue(field, required.toString().contains("\"" + field + "\""));
        }
    }
}
