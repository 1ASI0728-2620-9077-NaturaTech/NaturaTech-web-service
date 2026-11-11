package com.plantsync.platform.iot.interfaces.rest.resources;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LinkPlantResource(
    @NotNull @Min(1) Long plantId
) {
}
