package com.plantsync.platform.profiles.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 * The type User id.
 */
@Embeddable
public record UserId(Long value) {
  /**
   * Instantiates a new User id.
   *
   * @param value the value
   */
  public UserId {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException("Invalid profile id");
    }
  }
}
