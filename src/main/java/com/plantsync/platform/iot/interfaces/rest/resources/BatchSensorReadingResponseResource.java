package com.plantsync.platform.iot.interfaces.rest.resources;

import java.util.List;

public record BatchSensorReadingResponseResource(String message, List<Long> readingIds) {
}
