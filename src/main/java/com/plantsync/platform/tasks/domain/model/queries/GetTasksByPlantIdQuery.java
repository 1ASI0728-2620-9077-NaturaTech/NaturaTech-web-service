package com.plantsync.platform.tasks.domain.model.queries;

import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;

public record GetTasksByPlantIdQuery(PlantId plantId) {
}
