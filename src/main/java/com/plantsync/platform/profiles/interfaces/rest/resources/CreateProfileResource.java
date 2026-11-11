package com.plantsync.platform.profiles.interfaces.rest.resources;

/**
 * The type Create profile resource.
 */
public record CreateProfileResource(

    String personName,
    String subscriptionPlan,
    Long userId

) {
  /**
   * Instantiates a new Create profile resource.
   *
   * @param personName       the person name
   * @param subscriptionPlan the subscription plan
   * @param userId           the user id
   */
  public CreateProfileResource {

  }
}
