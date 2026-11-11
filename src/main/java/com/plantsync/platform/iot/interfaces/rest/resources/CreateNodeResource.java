package com.plantsync.platform.iot.interfaces.rest.resources;

public record CreateNodeResource(String nodeCode, Long plantId, Long profileId) {
}
