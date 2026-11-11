package com.plantsync.platform.iot.domain.model.queries;

import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;

public record GetAllNodesByProfileIdQuery(ProfileId profileId) {
}
