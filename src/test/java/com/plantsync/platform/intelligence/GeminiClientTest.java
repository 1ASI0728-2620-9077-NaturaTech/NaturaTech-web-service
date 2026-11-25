package com.plantsync.platform.intelligence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class GeminiClientTest {
  HttpClient http;
  HttpResponse<String> response;
  GeminiClient client;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setup() throws Exception {
    http = mock(HttpClient.class);
    response = mock(HttpResponse.class);
    when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(response);
    client =
        new GeminiClient(new ObjectMapper(), "test-only-placeholder", "gemini-flash-latest", http, millis -> {});
    when(response.statusCode()).thenReturn(200);
  }

  @Test
  void sendsHeaderAuthenticationAndParsesJson() throws Exception {
    when(response.body())
        .thenReturn(
            "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"{\\\"reply\\\":\\\"Hola\\\"}\"}]}}]}");
    assertEquals(
        "Hola", client.generate("system", List.of(Map.of("text", "hello"))).path("reply").asText());
    var capture = ArgumentCaptor.forClass(HttpRequest.class);
    verify(http).send(capture.capture(), any());
    assertEquals("POST", capture.getValue().method());
    assertTrue(
        capture.getValue().uri().toString().endsWith("/gemini-flash-latest:generateContent"));
    assertFalse(capture.getValue().uri().toString().contains("test-only"));
    assertEquals(
        "test-only-placeholder",
        capture.getValue().headers().firstValue("x-goog-api-key").orElseThrow());
  }

  @Test
  void imageRequestsRequireTheAnalysisSchema() throws Exception {
    when(response.body()).thenReturn("{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}");
    client.generate("analysis", List.of(Map.of("inlineData", Map.of("mimeType", "image/png", "data", "fixture"))));
    var capture = ArgumentCaptor.forClass(HttpRequest.class);
    verify(http).send(capture.capture(), any());
    var content = new java.io.ByteArrayOutputStream();
    var done = new java.util.concurrent.CompletableFuture<String>();
    capture.getValue().bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
      public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
      public void onNext(java.nio.ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes); content.writeBytes(bytes);
      }
      public void onError(Throwable error) { done.completeExceptionally(error); }
      public void onComplete() { done.complete(content.toString(java.nio.charset.StandardCharsets.UTF_8)); }
    });
    var config = new ObjectMapper().readTree(done.get()).path("generationConfig");
    var schema = config.path("responseSchema");
    assertEquals(5, schema.path("required").size());
    assertTrue(schema.path("properties").path("species").path("nullable").asBoolean());
    assertEquals(3, schema.path("properties").path("observations").path("maxItems").asInt());
    assertEquals("INCONCLUSIVE", schema.path("properties").path("status").path("enum").get(1).asText());
  }

  @Test
  void rejectsProviderErrorsWithoutLeakingResponse() {
    var cases = Map.of(401, "GEMINI_CREDENTIALS_REJECTED", 403, "GEMINI_CREDENTIALS_REJECTED",
        404, "GEMINI_MODEL_UNAVAILABLE", 429, "GEMINI_RATE_LIMITED",
        400, "GEMINI_REQUEST_REJECTED", 503, "GEMINI_UNAVAILABLE");
    when(response.body()).thenReturn("private provider details and credentials");
    cases.forEach((status, code) -> {
      when(response.statusCode()).thenReturn(status);
      var error = assertThrows(ResponseStatusException.class, () -> client.generate("x", List.of()));
      assertEquals(code, error.getReason());
      assertFalse(error.getMessage().contains("private provider"));
    });
  }

  @Test
  void rejectsIncompleteAndMalformedResults() {
    for (var body : List.of("{}", "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\"}]}")) {
      when(response.body()).thenReturn(body);
      assertEquals(
          "GEMINI_INCOMPLETE",
          assertThrows(ResponseStatusException.class, () -> client.generate("x", List.of()))
              .getReason());
    }
    when(response.body()).thenReturn("not-json");
    assertThrows(ResponseStatusException.class, () -> client.generate("x", List.of()));
  }

  @Test
  void retriesTemporaryOverloadAndRecovers() throws Exception {
    when(response.statusCode()).thenReturn(503, 503, 200);
    when(response.body()).thenReturn("{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"{\\\"reply\\\":\\\"Hola\\\"}\"}]}}]}");
    var delays = new ArrayList<Long>();
    client = new GeminiClient(new ObjectMapper(), "key", "model", http, delays::add);
    assertEquals("Hola", client.generate("x", List.of()).path("reply").asText());
    verify(http, times(3)).send(any(), any());
    assertEquals(2, delays.size());
    assertTrue(delays.get(0) >= 1000 && delays.get(0) < 1250);
    assertTrue(delays.get(1) >= 2000 && delays.get(1) < 2250);
  }

  @Test
  void neverRetriesInvalidCredentialsAndBoundsOverloadAttempts() throws Exception {
    when(response.statusCode()).thenReturn(403);
    assertThrows(ResponseStatusException.class, () -> client.generate("x", List.of()));
    verify(http, times(1)).send(any(), any());
    clearInvocations(http);
    when(response.statusCode()).thenReturn(503);
    assertThrows(ResponseStatusException.class, () -> client.generate("x", List.of()));
    verify(http, times(3)).send(any(), any());
  }

  @Test
  void respectsLongRetryAfterWithoutHoldingRequestOpen() throws Exception {
    when(response.statusCode()).thenReturn(429);
    when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Retry-After", List.of("60")), (a, b) -> true));
    var error = assertThrows(ResponseStatusException.class, () -> client.generate("x", List.of()));
    assertEquals("GEMINI_RATE_LIMITED", error.getReason());
    verify(http, times(1)).send(any(), any());
  }

  @Test
  void preservesInterruptionAndFailsClosedWhenUnconfigured() throws Exception {
    assertFalse(new GeminiClient(new ObjectMapper(), "", "model", http).available());
    assertThrows(
        ResponseStatusException.class,
        () -> new GeminiClient(new ObjectMapper(), "", "model", http).generate("x", List.of()));
    assertFalse(new GeminiClient(new ObjectMapper(), "key", "../unsafe", http).available());
    when(http.send(any(), any())).thenThrow(new InterruptedException());
    try {
      assertEquals(
          "GEMINI_INTERRUPTED",
          assertThrows(ResponseStatusException.class, () -> client.generate("x", List.of()))
              .getReason());
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }
}
