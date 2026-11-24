package com.plantsync.platform.intelligence;

import static org.springframework.http.HttpStatus.*;

import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Calls the real provider. No sample responses or credentials exposed to the browser. */
@Component
public class GeminiClient {
  private final ObjectMapper json;
  private final String key;
  private final String model;
  private final HttpClient http;
  private final Sleeper sleeper;

  @FunctionalInterface
  interface Sleeper {
    void sleep(long millis) throws InterruptedException;
  }

  @org.springframework.beans.factory.annotation.Autowired
  public GeminiClient(
      ObjectMapper json,
      @Value("${integrations.gemini.key:}") String key,
      @Value("${integrations.gemini.model:}") String model) {
    this(json, key, model, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }

  GeminiClient(ObjectMapper json, String key, String model, HttpClient http) {
    this(json, key, model, http, Thread::sleep);
  }

  GeminiClient(ObjectMapper json, String key, String model, HttpClient http, Sleeper sleeper) {
    this.http = http;
    this.json = json;
    this.key = key;
    this.model = model;
    this.sleeper = sleeper;
  }

  private HttpResponse<String> sendWithRetry(HttpRequest request) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
    for (int attempt = 0; attempt < 3; attempt++) {
      HttpResponse<String> response;
      try {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) throw new java.net.http.HttpTimeoutException("Provider retry budget exhausted");
        var bounded = HttpRequest.newBuilder(request, (name, value) -> true)
            .timeout(Duration.ofNanos(remaining)).build();
        response = http.send(bounded, HttpResponse.BodyHandlers.ofString());
      } catch (java.io.IOException error) {
        if (attempt == 2 || deadline - System.nanoTime() < Duration.ofSeconds(4).toNanos()) throw error;
        sleeper.sleep((1000L << attempt) + java.util.concurrent.ThreadLocalRandom.current().nextInt(250));
        continue;
      }
      if (attempt == 2 || !Set.of(429, 500, 502, 503, 504).contains(response.statusCode()))
        return response;
      long delay = (1000L << attempt) + java.util.concurrent.ThreadLocalRandom.current().nextInt(250);
      var headers = response.headers();
      var retryAfter = headers == null ? Optional.<String>empty() : headers.firstValue("Retry-After");
      if (retryAfter.isPresent()) {
        try {
          long requested = Math.multiplyExact(Long.parseLong(retryAfter.get()), 1000);
          // A synchronous request must not sleep past its bounded latency budget.
          if (requested > 3000 || requested < 0) return response;
          delay = Math.max(delay, requested);
        } catch (NumberFormatException | ArithmeticException ignored) {
          // HTTP-date or invalid values are not retried earlier than requested.
          return response;
        }
      }
      if (deadline - System.nanoTime() <= Duration.ofMillis(delay).toNanos()) return response;
      sleeper.sleep(delay);
    }
    throw new IllegalStateException("Retry budget exhausted");
  }

  public boolean available() {
    return !key.isBlank() && model.matches("[a-zA-Z0-9._-]+");
  }

  public JsonNode generate(String instruction, List<Map<String, Object>> parts) {
    if (!available())
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "GEMINI_NOT_CONFIGURED");
    try {
      var body =
          Map.of(
              "systemInstruction",
              Map.of("parts", List.of(Map.of("text", instruction))),
              "contents",
              List.of(Map.of("role", "user", "parts", parts)),
              "generationConfig",
              Map.of("responseMimeType", "application/json", "maxOutputTokens", 2048));
      var request =
          HttpRequest.newBuilder(
                  URI.create(
                      "https://generativelanguage.googleapis.com/v1beta/models/"
                          + model
                          + ":generateContent"))
              .timeout(Duration.ofSeconds(45))
              .header("Content-Type", "application/json")
              .header("x-goog-api-key", key)
              .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
              .build();
      var response = sendWithRetry(request);
      if (response.statusCode() != 200) {
        // Return stable diagnostic codes, never the provider body or API key.
        String code = switch (response.statusCode()) {
          case 401, 403 -> "GEMINI_CREDENTIALS_REJECTED";
          case 404 -> "GEMINI_MODEL_UNAVAILABLE";
          case 429 -> "GEMINI_RATE_LIMITED";
          case 400 -> "GEMINI_REQUEST_REJECTED";
          default -> "GEMINI_UNAVAILABLE";
        };
        org.slf4j.LoggerFactory.getLogger(GeminiClient.class)
            .warn("Gemini request rejected: upstreamStatus={}, code={}", response.statusCode(), code);
        throw new ResponseStatusException(BAD_GATEWAY, code);
      }
      var candidate = json.readTree(response.body()).path("candidates").path(0);
      if (!"STOP".equals(candidate.path("finishReason").asText()))
        throw new ResponseStatusException(BAD_GATEWAY, "GEMINI_INCOMPLETE");
      var output = candidate.path("content").path("parts").path(0).path("text").asText();
      if (output.length() > 16000)
        throw new ResponseStatusException(BAD_GATEWAY, "GEMINI_INVALID_RESULT");
      return json.readTree(output);
    } catch (ResponseStatusException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(BAD_GATEWAY, "GEMINI_INTERRUPTED");
    } catch (Exception e) {
      throw new ResponseStatusException(BAD_GATEWAY, "GEMINI_UNAVAILABLE");
    }
  }
}
