package com.kms3735.threelinesfirst.ai;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 개인정보를 가린 글을 Gemini로 보내 세 줄 요약을 받는다.
 * 응답은 responseSchema로 JSON 형식을 고정하고, 칸이 빠지거나 비면 한 번 더 요청한다.
 */
public class SummaryClient {

    public interface Callback {
        void onSuccess(Summary summary);

        void onFailure();
    }

    private static final String TAG = "SummaryClient";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    // 2026-11 기준. 샘플 5종에서 마감일을 모두 맞혔고 2~4초로 빠르다.
    // 이전 모델(gemini-2.5-flash)은 새 사용자에게 막혔으므로, 모델이 닫히면 이 이름만 바꾼다
    static final String MODEL = "gemini-3.5-flash-lite";
    /** 첫 요청이 실패하면 다시 요청할 때 쓰는 모델. 한 모델이 붐벼도(503) 요약을 받을 수 있게 */
    static final String FALLBACK_MODEL = "gemini-3.5-flash";

    static final String PROMPT = "너는 어르신이 공공 안내문을 쉽게 이해하도록 돕는 도우미야. 안내문을 읽고 아래 칸을 채워.\n"
            + "- line1_what: 무슨 안내문인지 한 문장 (예: 아파트 관리비 고지서예요.)\n"
            + "- line2_todo: 받은 사람이 해야 할 일 한 문장\n"
            + "- line3_when_where: 언제까지, 어디서, 얼마를, 어떻게 하는지 한 문장. 낼 돈이나 받을 돈이 있으면 금액을 꼭 넣어\n"
            + "- warning: 하지 않으면 생기는 불이익이나 꼭 주의할 점 한 문장. 안내문에 없으면 빈 문자열\n"
            + "- todos: 해야 할 일을 짧은 체크 항목으로 1~3개 (예: 관리비 내기)\n"
            + "- deadline: 가장 중요한 마감일을 YYYY-MM-DD로. 연도가 없으면 오늘 날짜를 기준으로 정하고, 마감일이 없으면 빈 문자열\n"
            + "- amounts: 내야 하거나 받는 금액을 원 단위 정수로. 없으면 빈 배열\n"
            + "- phone: 문의할 기관 전화번호를 02-123-4567처럼. [휴대전화]처럼 가린 번호는 넣지 말고, 없으면 빈 문자열\n"
            + "- place: 받은 사람이 직접 가야 하는 곳이 있으면 지도에서 찾을 수 있는 기관 이름 (예: 종로구 보건소). 없거나 여러 곳 중 고르는 경우는 빈 문자열\n"
            + "규칙:\n"
            + "- 한 문장은 40자 안팎으로, 쉬운 말과 '~해요' 말투로 써. 문장 속 금액은 152,000원처럼 쉼표를 넣어.\n"
            + "- 안내문에 없는 내용은 지어내지 마. 날짜와 금액은 안내문에 적힌 값을 그대로 옮겨.\n"
            + "- [이름], [주소], [계좌번호] 같은 표시는 개인정보를 가린 것이니 추측하지 마.\n"
            + "- 글자 인식 과정에서 글자가 조금 틀렸을 수 있으니 문맥으로 읽어.";

    private static final String SCHEMA = "{\"type\":\"OBJECT\",\"properties\":{"
            + "\"line1_what\":{\"type\":\"STRING\"},"
            + "\"line2_todo\":{\"type\":\"STRING\"},"
            + "\"line3_when_where\":{\"type\":\"STRING\"},"
            + "\"warning\":{\"type\":\"STRING\"},"
            + "\"todos\":{\"type\":\"ARRAY\",\"items\":{\"type\":\"STRING\"}},"
            + "\"deadline\":{\"type\":\"STRING\"},"
            + "\"amounts\":{\"type\":\"ARRAY\",\"items\":{\"type\":\"INTEGER\"}},"
            + "\"phone\":{\"type\":\"STRING\"},"
            + "\"place\":{\"type\":\"STRING\"}},"
            + "\"required\":[\"line1_what\",\"line2_todo\",\"line3_when_where\",\"warning\",\"todos\",\"deadline\",\"amounts\",\"phone\",\"place\"],"
            + "\"propertyOrdering\":[\"line1_what\",\"line2_todo\",\"line3_when_where\",\"warning\",\"todos\",\"deadline\",\"amounts\",\"phone\",\"place\"]}";

    private final GeminiApi api;
    private final String key;

    public SummaryClient(String key) {
        this.key = key;
        // 붐빌 때는 10초를 넘길 때가 있어 기본 읽기 시간(10초)을 늘린다
        OkHttpClient http = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();
        api = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeminiApi.class);
    }

    /** 결과는 메인 스레드의 callback으로 돌아온다 */
    public void summarize(String maskedText, LocalDate today, Callback callback) {
        summarize(maskedText, today, null, callback);
    }

    /** note: 다시 요약할 때 덧붙이는 주의 사항 (예: 원문에 없던 날짜). 없으면 null */
    public void summarize(String maskedText, LocalDate today, String note, Callback callback) {
        request(buildRequest(maskedText, today, note), callback, 1);
    }

    private void request(JsonObject body, Callback callback, int retriesLeft) {
        String model = retriesLeft > 0 ? MODEL : FALLBACK_MODEL;
        api.generate(model, key, body).enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Summary summary = response.isSuccessful() ? parse(response.body()) : null;
                if (summary != null) {
                    callback.onSuccess(summary);
                    return;
                }
                // 형식이 틀렸거나, 서버가 붐비거나(5xx), 모델의 무료 사용량을 넘었을 때(429)
                // 다른 모델로 한 번 더 요청한다. 키가 틀린 경우(400·403)는 다시 해도 같다
                boolean retry = response.isSuccessful() || response.code() >= 500 || response.code() == 429;
                Log.w(TAG, model + " 요약 실패 HTTP " + response.code() + (retry && retriesLeft > 0 ? ", 다시 요청" : ""));
                if (retry && retriesLeft > 0) {
                    request(body, callback, retriesLeft - 1);
                } else {
                    callback.onFailure();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // 인터넷이 끊기면 onFailure, 시간 초과면 다른 모델로 한 번 더
                Log.w(TAG, model + " 요약 요청 실패", t);
                if (t instanceof java.net.SocketTimeoutException && retriesLeft > 0) {
                    request(body, callback, retriesLeft - 1);
                } else {
                    callback.onFailure();
                }
            }
        });
    }

    static JsonObject buildRequest(String maskedText, LocalDate today, String note) {
        JsonObject body = new JsonObject();
        body.add("systemInstruction", content(PROMPT));

        String text = "오늘 날짜: " + today + "\n\n[안내문]\n" + maskedText;
        if (note != null) text += "\n\n[다시 확인]\n" + note;
        JsonObject user = content(text);
        user.addProperty("role", "user");
        JsonArray contents = new JsonArray();
        contents.add(user);
        body.add("contents", contents);

        JsonObject config = new JsonObject();
        config.addProperty("responseMimeType", "application/json");
        config.add("responseSchema", JsonParser.parseString(SCHEMA));
        config.addProperty("temperature", 0.2);
        // 짧은 요약이라 생각 단계를 가장 짧게 해서 빨리 받는다 (두 모델 모두 지원)
        JsonObject thinking = new JsonObject();
        thinking.addProperty("thinkingLevel", "minimal");
        config.add("thinkingConfig", thinking);
        body.add("generationConfig", config);
        return body;
    }

    private static JsonObject content(String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        return content;
    }

    /** 응답에서 요약 JSON을 꺼내 형식을 검사한다. 형식이 틀리면 null */
    static Summary parse(JsonObject response) {
        try {
            String json = response.getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString();
            Summary summary = new Gson().fromJson(json, Summary.class);
            if (summary == null || !summary.isValid()) return null;
            summary.todos.removeIf(t -> t == null || t.trim().isEmpty());
            // 전화·장소는 없어도 요약은 쓸 수 있으니 빠지면 빈 칸으로 둔다
            if (summary.phone == null) summary.phone = "";
            if (summary.place == null) summary.place = "";
            return summary;
        } catch (RuntimeException e) {
            // 칸이 없거나(NullPointer), JSON이 깨졌거나(JsonParseException), 숫자가 아닌 경우
            return null;
        }
    }
}
