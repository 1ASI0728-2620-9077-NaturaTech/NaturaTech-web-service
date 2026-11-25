package com.plantsync.platform.plantprofiles.interfaces.rest.assemblers;

import static org.junit.jupiter.api.Assertions.*;
import com.plantsync.platform.plantprofiles.interfaces.rest.resources.CreatePlantResource;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CreatePlantDatesTest {
  @Test void updateAcceptsMissingOptionalWateringDate() {
    for (String date : new String[] {null, "", " "}) {
      var input = new com.plantsync.platform.plantprofiles.interfaces.rest.resources.UpdatePlantResource(
          "Pothos", "Epipremnum", "", "2026-11-11", "MEDIA", date, null, true,
          null, null, null, null, null, 1L);
      assertNull(UpdatePlantCommandFromResourceAssembler.toCommandFromResource(1L, input).nextWateringDate());
    }
  }
  @Test void updateRejectsInvalidRequiredDateWith400() {
    var input = new com.plantsync.platform.plantprofiles.interfaces.rest.resources.UpdatePlantResource(
        "Pothos", "Epipremnum", "", "invalid", "MEDIA", null, null, true,
        null, null, null, null, null, 1L);
    var error = assertThrows(ResponseStatusException.class,
        () -> UpdatePlantCommandFromResourceAssembler.toCommandFromResource(1L, input));
    assertEquals(400, error.getStatusCode().value());
  }
  private CreatePlantResource input(String arrival, String watering) {
    return new CreatePlantResource("Pothos", "Epipremnum", "", arrival, "MEDIA", watering,
        null, true, null, null, null, null, null, 1L);
  }
  @Test void acceptsMissingOptionalWateringDate() {
    for (String date : new String[] {null, "", " "})
      assertNull(CreatePlantCommandFromResourceAssembler.toCommandFromResource(input("2026-11-11", date)).nextWateringDate());
  }
  @Test void rejectsInvalidDatesAsClientErrors() {
    for (var resource : new CreatePlantResource[] {input(null, null), input("invalid", null), input("2026-11-11", "invalid")}) {
      var error = assertThrows(ResponseStatusException.class, () -> CreatePlantCommandFromResourceAssembler.toCommandFromResource(resource));
      assertEquals(400, error.getStatusCode().value());
    }
  }
  @Test void serializesPlantWithoutOptionalWateringDate() {
    var command = CreatePlantCommandFromResourceAssembler.toCommandFromResource(input("2026-11-11", null));
    var plant = new com.plantsync.platform.plantprofiles.domain.model.aggregates.Plant(command);
    var resource = PlantResourceFromEntityAssembler.toResourceFromEntity(plant);
    assertNull(resource.nextWateringDate());
    assertEquals("Pothos", resource.name());
  }
}
