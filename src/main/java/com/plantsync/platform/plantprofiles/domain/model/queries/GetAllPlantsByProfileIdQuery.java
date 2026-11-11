package com.plantsync.platform.plantprofiles.domain.model.queries;

import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;

/**
 * The type Get all plants by profile id query.
 */
public record GetAllPlantsByProfileIdQuery(ProfileId profileId) {
}
