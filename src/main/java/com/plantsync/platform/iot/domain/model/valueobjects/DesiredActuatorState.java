package com.plantsync.platform.iot.domain.model.valueobjects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record DesiredActuatorState(
    @Enumerated(EnumType.STRING)
    @Column(name = "desired_buzzer_mode", nullable = false, length = 20)
    BuzzerMode buzzerMode,

    @Enumerated(EnumType.STRING)
    @Column(name = "desired_servo_mode", nullable = false, length = 20)
    ServoMode servoMode,

    @Enumerated(EnumType.STRING)
    @Column(name = "desired_led_mode", nullable = false, length = 20)
    LedMode ledMode
) {
  public DesiredActuatorState() {
    this(BuzzerMode.OFF, ServoMode.OFF, LedMode.OFF);
  }
}
