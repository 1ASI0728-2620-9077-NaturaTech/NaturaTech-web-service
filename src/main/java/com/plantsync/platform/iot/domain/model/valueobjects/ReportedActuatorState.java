package com.plantsync.platform.iot.domain.model.valueobjects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;

@Embeddable
public record ReportedActuatorState(
    @Enumerated(EnumType.STRING)
    @Column(name = "reported_buzzer_mode", nullable = false, length = 20)
    BuzzerMode buzzerMode,

    @Enumerated(EnumType.STRING)
    @Column(name = "reported_servo_mode", nullable = false, length = 20)
    ServoMode servoMode,

    @Enumerated(EnumType.STRING)
    @Column(name = "reported_led_mode", nullable = false, length = 20)
    LedMode ledMode,

    @Column(name = "reported_at")
    LocalDateTime timestamp
) {
  public ReportedActuatorState(BuzzerMode buzzerMode, ServoMode servoMode, LedMode ledMode) {
    this(buzzerMode, servoMode, ledMode, LocalDateTime.now());
  }
}
