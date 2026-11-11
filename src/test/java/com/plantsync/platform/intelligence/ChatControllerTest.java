package com.plantsync.platform.intelligence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.shared.security.ResourceAccess;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;

class ChatControllerTest {
  GeminiClient gemini = mock(GeminiClient.class);
  ResourceAccess access = mock(ResourceAccess.class);
  AiQuotaService quota = mock(AiQuotaService.class);
  ChatController chat = new ChatController(gemini, access, quota);

  @Test
  void ignoresClientSystemInstructionsAndConsumesQuota() throws Exception {
    var user = mock(User.class);
    when(user.getId()).thenReturn(7L);
    when(access.currentUser()).thenReturn(user);
    when(gemini.available()).thenReturn(true);
    when(gemini.generate(anyString(), anyList()))
        .thenReturn(new ObjectMapper().readTree("{\"reply\":\"Cuida tu planta\"}"));
    var result =
        chat.chat(
            new ChatController.Request(
                List.of(
                    new ChatController.Message("system", "UNTRUSTED OVERRIDE"),
                    new ChatController.Message("user", "Hola"))));
    assertEquals(Map.of("reply", "Cuida tu planta"), result);
    verify(quota).consume(7L);
    verify(gemini)
        .generate(anyString(), argThat(parts -> !parts.toString().contains("UNTRUSTED OVERRIDE")));
    when(gemini.generate(anyString(), anyList()))
        .thenReturn(new ObjectMapper().createObjectNode().put("reply", ""));
    assertEquals(
        "CHAT_INVALID_RESULT",
        assertThrows(
                ResponseStatusException.class,
                () ->
                    chat.chat(
                        new ChatController.Request(
                            List.of(new ChatController.Message("user", "Hi")))))
            .getReason());
  }

  @Test
  void rejectsMalformedHistoryAndUnavailableProvider() {
    for (var messages :
        List.of(
            List.<ChatController.Message>of(),
            List.of(new ChatController.Message("system", "only system")),
            List.of(new ChatController.Message(null, "x")),
            List.of(new ChatController.Message("user", "x".repeat(3001)))))
      assertThrows(
          ResponseStatusException.class, () -> chat.chat(new ChatController.Request(messages)));
    assertThrows(ResponseStatusException.class, () -> chat.chat(new ChatController.Request(null)));
    assertEquals(
        "GEMINI_NOT_CONFIGURED",
        assertThrows(
                ResponseStatusException.class,
                () ->
                    chat.chat(
                        new ChatController.Request(
                            List.of(new ChatController.Message("user", "Hi")))))
            .getReason());
    verifyNoInteractions(quota);
  }
}
