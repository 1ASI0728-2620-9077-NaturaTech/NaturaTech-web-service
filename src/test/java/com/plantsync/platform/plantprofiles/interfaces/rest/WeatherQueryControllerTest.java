package com.plantsync.platform.plantprofiles.interfaces.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class WeatherQueryControllerTest {
  @Test
  void encodesCityAsDataAndKeepsExistingSuccessBody() {
    var client = new RestTemplate();
    var server = MockRestServiceServer.bindTo(client).build();
    var controller = new WeatherQueryController(client);
    ReflectionTestUtils.setField(controller, "apiKey", "test-key");
    server.expect(requestTo("https://api.openweathermap.org/data/2.5/weather?q=Lima%26appid%3Devil&appid=test-key&units=metric"))
        .andRespond(withSuccess("{\"name\":\"Lima\"}", MediaType.APPLICATION_JSON));
    assertEquals("{\"name\":\"Lima\"}", controller.getWeatherByCity("Lima&appid=evil").getBody());
    server.verify();
  }

  @Test
  void providerErrorsNeverReturnCredentialsOrProviderBody() {
    var client = new RestTemplate();
    var server = MockRestServiceServer.bindTo(client).build();
    var controller = new WeatherQueryController(client);
    ReflectionTestUtils.setField(controller, "apiKey", "test-key");
    server.expect(requestTo("https://api.openweathermap.org/data/2.5/weather?q=Lima&appid=test-key&units=metric"))
        .andRespond(withServerError().body("private-provider-information test-key"));
    var result = controller.getWeatherByCity("Lima");
    assertEquals(502, result.getStatusCode().value());
    assertEquals(java.util.Map.of("code", "WEATHER_PROVIDER_ERROR"), result.getBody());
    server.verify();
  }

  @Test
  void missingConfigurationAndInvalidCityFailBeforeNetwork() {
    var controller = new WeatherQueryController();
    assertEquals(503, controller.getWeatherByCity("Lima").getStatusCode().value());
    ReflectionTestUtils.setField(controller, "apiKey", "test-key");
    for (String city : new String[] {null, " ", "a".repeat(121)}) {
      assertEquals(400, controller.getWeatherByCity(city).getStatusCode().value());
    }
  }
}
