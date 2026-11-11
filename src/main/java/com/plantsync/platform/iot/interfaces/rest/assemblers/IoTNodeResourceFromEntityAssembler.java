package com.plantsync.platform.iot.interfaces.rest.assemblers;

import com.plantsync.platform.iot.domain.model.aggregates.IoTNode;
import com.plantsync.platform.iot.interfaces.rest.resources.IoTNodeResource;
import java.text.SimpleDateFormat;

public class IoTNodeResourceFromEntityAssembler {

  public static IoTNodeResource toResourceFromEntity(IoTNode entity) {
    return new IoTNodeResource(
        entity.getId(),
        entity.getNodeCode(),
        entity.getStatus().name(),
        entity.getPlantId() != null ? entity.getPlantId().value() : null,
        entity.getProfileId() != null ? entity.getProfileId().value() : null,
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .format(entity.getCreatedAt())
    );
  }
}
