package com.plantsync.platform.profiles.domain.services;

import com.plantsync.platform.profiles.domain.model.aggregates.Profile;
import com.plantsync.platform.profiles.domain.model.queries.GetAllProfilesQuery;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByIdQuery;
import com.plantsync.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import java.util.List;
import java.util.Optional;

/**
 * The interface Profile query service.
 */
public interface ProfileQueryService {
  /**
   * Handle Get Profile By ID Query.
   *
   * @param query The {@link GetProfileByIdQuery} Query
   * @return A {@link Profile} instance if the query is valid, otherwise empty
   */
  Optional<Profile> handle(GetProfileByIdQuery query);

  /**
   * Handle optional.
   *
   * @param query the query
   * @return the optional
   */
  Optional<Profile> handle(GetProfileByUserIdQuery query);

  /**
   * Handle list.
   *
   * @param query the query
   * @return the list
   */
  List<Profile> handle(GetAllProfilesQuery query);

}