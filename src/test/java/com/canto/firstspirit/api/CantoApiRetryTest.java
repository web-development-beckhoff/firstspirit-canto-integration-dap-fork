package com.canto.firstspirit.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.canto.firstspirit.service.cache.ProjectBoundCacheAccess;
import java.io.IOException;
import java.lang.reflect.Field;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CantoApiRetryTest {

  private static final HttpUrl TEST_URL = HttpUrl.parse("https://test.canto.global/api/v1/image/abc");
  private static final int RETRY_COUNT = 3;

  private CantoApi cantoApi;
  private Call mockCall;

  @BeforeEach
  void setUp() throws Exception {
    cantoApi = new CantoApi.Builder()
      .tenant("test.canto.global")
      .oAuthBaseUrl("https://oauth.canto.global")
      .appId("appId")
      .appSecret("appSecret")
      .userId("userId")
      .projectBoundCacheAccess(new ProjectBoundCacheAccess(null))
      .timeoutInSeconds(20)
      .rateLimitRetryCount(RETRY_COUNT)
      .build();

    OkHttpClient mockClient = mock(OkHttpClient.class);
    mockCall = mock(Call.class);
    when(mockClient.newCall(any())).thenReturn(mockCall);

    setField("okHttpClient", mockClient);
    setField("validUntilTimestamp", Long.MAX_VALUE);
    setField("retryDelayMs", 0L);
  }

  @Test
  void successOnFirstTry_noRetry() throws Exception {
    Response r200 = response(200);
    when(mockCall.execute()).thenReturn(r200);

    Response result = cantoApi.executeGetRequest(TEST_URL);

    assertEquals(200, result.code());
    verify(mockCall, times(1)).execute();
  }

  @Test
  void oneRateLimit_thenSuccess() throws Exception {
    Response r429 = response(429);
    Response r200 = response(200);
    when(mockCall.execute()).thenReturn(r429, r200);

    Response result = cantoApi.executeGetRequest(TEST_URL);

    assertEquals(200, result.code());
    verify(mockCall, times(2)).execute();
  }

  @Test
  void lastRetrySucceeds() throws Exception {
    // initial + (RETRY_COUNT - 1) failed retries + 1 successful retry
    Response r429a = response(429);
    Response r429b = response(429);
    Response r429c = response(429);
    Response r200 = response(200);
    when(mockCall.execute()).thenReturn(r429a, r429b, r429c, r200);

    Response result = cantoApi.executeGetRequest(TEST_URL);

    assertEquals(200, result.code());
    verify(mockCall, times(RETRY_COUNT + 1)).execute();
  }

  @Test
  void allRetriesExhausted_throwsIOException() throws Exception {
    // initial + RETRY_COUNT retries, all 429
    Response r429a = response(429);
    Response r429b = response(429);
    Response r429c = response(429);
    Response r429d = response(429);
    when(mockCall.execute()).thenReturn(r429a, r429b, r429c, r429d);

    assertThrows(IOException.class, () -> cantoApi.executeGetRequest(TEST_URL));
    verify(mockCall, times(RETRY_COUNT + 1)).execute();
  }

  private Response response(int code) {
    return new Response.Builder()
        .code(code)
        .message(code == 200 ? "OK" : "Too Many Requests")
        .request(new Request.Builder().url(TEST_URL).build())
        .protocol(Protocol.HTTP_1_1)
        .body(ResponseBody.create("", MediaType.get("application/json")))
        .build();
  }

  private void setField(String name, Object value) throws Exception {
    Field field = CantoApi.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(cantoApi, value);
  }

}
