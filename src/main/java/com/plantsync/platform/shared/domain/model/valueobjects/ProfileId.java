package com.plantsync.platform.shared.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record ProfileId(Long value) {
  public ProfileId {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException("Invalid profile id");
    }
  }
}
