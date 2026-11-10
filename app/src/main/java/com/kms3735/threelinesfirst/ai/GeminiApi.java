package com.kms3735.threelinesfirst.ai;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Gemini REST API. 키는 주소가 아니라 헤더로 보낸다 */
public interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    Call<JsonObject> generate(@Path("model") String model,
                              @Header("x-goog-api-key") String key,
                              @Body JsonObject body);
}
