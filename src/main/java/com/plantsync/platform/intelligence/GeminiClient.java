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

  @org.springframework.beans.factory.annotation.Autowired
  public GeminiClient(
      ObjectMapper json,
      @Value("${integrations.gemini.key:}") String key,
      @Value("${integrations.gemini.model:}") String model) {
    this(json, key, model, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }

  GeminiClient(ObjectMapper json, String key, String model, HttpClient http) {
    this.http = http;
    this.json = json;
    this.key = key;
    this.model = model;
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
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200)
        throw new ResponseStatusException(BAD_GATEWAY, "GEMINI_UNAVAILABLE");
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
