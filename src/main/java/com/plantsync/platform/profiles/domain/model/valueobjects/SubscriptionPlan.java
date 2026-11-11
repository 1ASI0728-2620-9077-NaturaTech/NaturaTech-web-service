package com.plantsync.platform.profiles.domain.model.valueobjects;

/**
 * The enum Subscription plan.
 */
public enum SubscriptionPlan {
  /**
   * Basic subscription plan.
   */
  BASIC,
  /**
   * Premium subscription plan.
   */
  PREMIUM,
  /**
   * Pro subscription plan.
   */
  PRO;

  /**
   * From string subscription plan.
   *
   * @param value the value
   * @return the subscription plan
   */
  public static SubscriptionPlan fromString(String value) {
    try {
      return SubscriptionPlan.valueOf(value.toUpperCase()); // ← clave
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Invalid subscription plan: " + value);
    }
  }

}
