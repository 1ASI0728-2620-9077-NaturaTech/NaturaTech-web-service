package com.plantsync.platform.shared.interfaces.rest;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for REST controllers. Handles validation errors and other common
 * exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
  public ResponseEntity<?> invalidEntity() {
    return ResponseEntity.badRequest()
        .body(Map.of("code", "VALIDATION_FAILED", "message", "Revisa los datos enviados."));
  }

  @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
  public ResponseEntity<?> invalidCredentials() {
    return ResponseEntity.status(401)
        .body(Map.of("code", "INVALID_CREDENTIALS", "message", "Correo o contraseña incorrectos."));
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<?> forbidden() {
    return ResponseEntity.status(403)
        .body(Map.of("code", "ACCESS_DENIED", "message", "No tienes permiso para esta operación."));
  }

  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException error) {
    return ResponseEntity.status(error.getStatusCode())
        .body(
            Map.of(
                "code",
                error.getReason() == null ? "REQUEST_FAILED" : error.getReason(),
                "message",
                "No se pudo completar la operación.",
                "requestId",
                java.util.UUID.randomUUID().toString()));
  }

  /**
   * Handles validation errors from {@link MethodArgumentNotValidException}.
   *
   * @param ex The exception to handle.
   * @return A {@link ResponseEntity} containing a map of field errors.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationErrors(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
    return ResponseEntity.badRequest().body(errors);
  }
}
