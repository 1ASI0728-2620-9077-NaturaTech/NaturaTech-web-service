package com.plantsync.platform.profiles.domain.model.valueobjects;

/**
 * The type Person name.
 */
public record PersonName(String name) {

  /**
   * Instantiates a new Person name.
   *
   * @param name the name
   */
  public PersonName {

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be null or blank");
    }
  }
}