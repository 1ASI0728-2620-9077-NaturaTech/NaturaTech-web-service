package com.plantsync.platform.iot.domain.model.commands;

import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.shared.domain.model.valueobjects.ProfileId;

public record CreateNodeCommand(String nodeCode, PlantId plantId, ProfileId profileId) {
}
