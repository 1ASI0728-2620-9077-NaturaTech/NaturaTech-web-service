package com.plantsync.platform.profiles.interfaces.acl;

/**
 * ProfilesContextFacade.
 */
public interface ProfilesContextFacade {

  /**
   * Create profile long.
   *
   * @param name             the name
   * @param userId           the user id
   * @param subscriptionPlan the subscription plan
   * @return the long
   */
  Long createProfile(String name, Long userId, String subscriptionPlan);
  
}