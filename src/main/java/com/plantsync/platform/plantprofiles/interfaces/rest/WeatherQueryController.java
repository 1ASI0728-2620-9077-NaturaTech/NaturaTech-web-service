package com.plantsync.platform.plantprofiles.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;

/**
 * The type Weather query controller.
 */
@RestController
@RequestMapping("/api/v1/weather")
@Tag(name = "Weather", description = "Weather Proxy Endpoint")
public class WeatherQueryController {

  private final RestTemplate restTemplate;

  public WeatherQueryController() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(10000);
    restTemplate = new RestTemplate(factory);
  }

  WeatherQueryController(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Value("${weather.api.key}")
  private String apiKey;

  /**
   * Gets weather by city.
   *
   * @param city the city
   * @return the weather by city
   */
  @GetMapping("/city")
  public ResponseEntity<?> getWeatherByCity(@RequestParam String city) {
    if (apiKey == null || apiKey.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("code", "WEATHER_NOT_CONFIGURED"));
    }
    if (city == null || city.isBlank() || city.length() > 120) {
      return ResponseEntity.badRequest().body(Map.of("code", "CITY_INVALID"));
    }
    var url = UriComponentsBuilder.fromUriString("https://api.openweathermap.org/data/2.5/weather")
        .queryParam("q", "{city}").queryParam("appid", "{key}").queryParam("units", "metric")
        .encode().buildAndExpand(city.trim(), apiKey).toUri();

    try {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      return ResponseEntity.ok().body(response.getBody());
    } catch (Exception e) {
      return ResponseEntity.status(
          HttpStatus.BAD_GATEWAY).body(Map.of("code", "WEATHER_PROVIDER_ERROR"));
    }
  }
}
