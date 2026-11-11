package com.plantsync.platform.regression;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.plantsync.platform.analytics.application.internal.queryservices.AnalyticsQueryServiceImpl;
import com.plantsync.platform.analytics.domain.model.queries.*;
import com.plantsync.platform.iot.application.internal.commandservices.*;
import com.plantsync.platform.iot.domain.model.aggregates.IoTNode;
import com.plantsync.platform.iot.domain.model.commands.*;
import com.plantsync.platform.iot.domain.model.entities.*;
import com.plantsync.platform.iot.domain.model.queries.*;
import com.plantsync.platform.iot.domain.model.valueobjects.*;
import com.plantsync.platform.iot.domain.services.*;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.*;
import com.plantsync.platform.plantprofiles.application.internal.commandservices.PlantCommandServiceImpl;
import com.plantsync.platform.plantprofiles.domain.model.aggregates.Plant;
import com.plantsync.platform.plantprofiles.domain.model.commands.*;
import com.plantsync.platform.plantprofiles.domain.model.queries.GetPlantByIdQuery;
import com.plantsync.platform.plantprofiles.domain.model.valueobjects.*;
import com.plantsync.platform.plantprofiles.domain.services.PlantQueryService;
import com.plantsync.platform.plantprofiles.infrastructure.persistence.jpa.repositories.PlantRepository;
import com.plantsync.platform.shared.domain.model.valueobjects.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class LegacyServicesTest {
  @Test
  void plantCreateUpdateDeletePreservesFieldsAndReportsStorageFailures() {
    var repo = mock(PlantRepository.class);
    var service = new PlantCommandServiceImpl(repo);
    when(repo.save(any()))
        .thenAnswer(
            call -> {
              Plant p = call.getArgument(0);
              org.springframework.test.util.ReflectionTestUtils.setField(p, "id", 7L);
              return p;
            });
    var create =
        new CreatePlantCommand(
            new PlantName("Monstera"),
            "species",
            "description",
            null,
            HumidityLevel.MEDIA,
            null,
            "image",
            true,
            10f,
            35f,
            20f,
            30f,
            70f,
            new ProfileId(2L));
    assertEquals(7L, service.handle(create));
    Plant plant = new Plant(create);
    when(repo.findById(7L)).thenReturn(Optional.of(plant));
    var update =
        new UpdatePlantCommand(
            7L,
            new PlantName("Updated"),
            "other",
            "new",
            null,
            HumidityLevel.ALTA,
            null,
            "new-image",
            false,
            11f,
            34f,
            21f,
            31f,
            69f,
            new ProfileId(2L));
    var result = service.handle(update).orElseThrow();
    assertEquals("other", result.getSpecies());
    assertEquals(69f, result.getHumidityThresholdMax());
    when(repo.findById(7L)).thenReturn(Optional.empty());
    assertTrue(service.handle(update).isEmpty());
    assertThrows(RuntimeException.class, () -> service.handle(new DeletePlantCommand(7L)));
    when(repo.existsById(7L)).thenReturn(true);
    service.handle(new DeletePlantCommand(7L));
    verify(repo).deleteById(7L);
    doThrow(new IllegalStateException("database unavailable")).when(repo).deleteById(7L);
    assertThrows(RuntimeException.class, () -> service.handle(new DeletePlantCommand(7L)));
    doThrow(new IllegalStateException("database unavailable")).when(repo).save(any());
    assertThrows(RuntimeException.class, () -> service.handle(create));
    when(repo.findById(7L)).thenReturn(Optional.of(plant));
    assertThrows(RuntimeException.class, () -> service.handle(update));
  }

  @Test
  void nodeCreationAndRelinkingRejectDuplicatesAndInvalidateContinuity() {
    var repo = mock(IotNodeRepository.class);
    var service = new IoTNodeCommandServiceImpl(repo);
    when(repo.save(any())).thenAnswer(call -> call.getArgument(0));
    var command = new CreateNodeCommand("SIM", new PlantId(3L), new ProfileId(2L));
    var node = service.handle(command).orElseThrow();
    assertEquals(NodeStatus.ONLINE, node.getStatus());
    when(repo.findByNodeCode("SIM")).thenReturn(Optional.of(node));
    assertTrue(service.handle(command).isEmpty());
    when(repo.findByNodeCode("SIM")).thenReturn(Optional.empty());
    when(repo.findByPlantIdValue(3L)).thenReturn(Optional.of(node));
    assertTrue(service.handle(command).isEmpty());
    assertTrue(service.linkPlant("missing", 4L).isEmpty());
    assertTrue(service.unlinkPlant("missing").isEmpty());
    when(repo.findByNodeCode("SIM")).thenReturn(Optional.of(node));
    assertThrows(IllegalStateException.class, () -> service.linkPlant("SIM", 3L));
    assertEquals(4L, service.linkPlant("SIM", 4L).orElseThrow().getPlantId().value());
    assertNotNull(node.getLinkedAt());
    assertNull(service.unlinkPlant("SIM").orElseThrow().getPlantId());
    assertNotNull(node.getLinkedAt());
  }

  @Test
  void telemetryTriggersOnlyBuzzerAndHandlesUnlinkedOrMissingNodes() {
    var readings = mock(IotSensorReadingRepository.class);
    var commands = mock(ActuatorCommandService.class);
    var query = mock(IoTNodeQueryService.class);
    var plants = mock(PlantQueryService.class);
    var nodes = mock(IotNodeRepository.class);
    when(readings.save(any())).thenAnswer(call -> call.getArgument(0));
    var service = new SensorReadingCommandServiceImpl(readings, commands, query, plants, nodes);
    assertNotNull(service.handle(new CreateReadingCommand(1L, 40, 10, 23f, 45)));
    var node = new IoTNode(new CreateNodeCommand("SIM", null, new ProfileId(2L)));
    when(query.handle(any(GetNodeByIdQuery.class))).thenReturn(Optional.of(node));
    service.handle(new CreateReadingCommand(1L, 40, 50, 23f, 45));
    assertEquals(BuzzerMode.ON, node.getDesiredState().buzzerMode());
    node.setPlantId(new PlantId(3L));
    service.handle(new CreateReadingCommand(1L, 40, 10, 23f, 45));
    var plant = new Plant();
    plant.setTemperatureThresholdMin(10f);
    plant.setTemperatureThresholdMax(30f);
    when(plants.handle(any(GetPlantByIdQuery.class))).thenReturn(Optional.of(plant));
    service.handle(new CreateReadingCommand(1L, 0, 10, 5f, 0));
    service.handle(new CreateReadingCommand(1L, 0, 10, 40f, 0));
    service.handle(new CreateReadingCommand(1L, 40, null, null, 45));
    verify(commands, times(3))
        .handle(argThat((CreateActuatorCommand c) -> c.actuatorType() == ActuatorType.BUZZER));
    assertNotEquals(ServoMode.ON, node.getDesiredState().servoMode());
    assertNotEquals(LedMode.ON, node.getDesiredState().ledMode());
  }

  @Test
  void manualShutdownWorksForLegacyActuatorAliases() {
    var query = mock(IoTNodeQueryService.class);
    var commands = mock(ActuatorCommandService.class);
    var nodes = mock(IotNodeRepository.class);
    var service = new ActuatorCommandManualService(query, commands, nodes);
    assertThrows(
        IllegalArgumentException.class,
        () -> service.handle("missing", ActuatorType.BUZZER, ActuatorAction.ACTIVATE));
    var node = new IoTNode();
    org.springframework.test.util.ReflectionTestUtils.setField(node, "id", 1L);
    when(query.handle(any(GetNodeByNodeCodeQuery.class))).thenReturn(Optional.of(node));
    when(commands.handle(any(CreateActuatorCommand.class)))
        .thenAnswer(call -> new ActuatorCommand(call.getArgument(0)));
    service.handle("SIM", ActuatorType.BUZZER, ActuatorAction.ACTIVATE);
    assertEquals(BuzzerMode.ON, node.getDesiredState().buzzerMode());
    for (var type : ActuatorType.values()) service.handle("SIM", type, ActuatorAction.DEACTIVATE);
    assertEquals(BuzzerMode.OFF, node.getDesiredState().buzzerMode());
    assertEquals(ServoMode.OFF, node.getDesiredState().servoMode());
    assertEquals(LedMode.OFF, node.getDesiredState().ledMode());
  }

  @Test
  void analyticsHandlesEmptyGardenAndActualAggregates() {
    var readings = mock(IotSensorReadingRepository.class);
    var nodes = mock(IotNodeRepository.class);
    var service = new AnalyticsQueryServiceImpl(readings, nodes);
    assertEquals(0.0, service.handle(new GetGlobalTelemetryQuery()).averageHumidityPercent());
    when(readings.findAverageHumidityPercent()).thenReturn(45.0);
    when(readings.findAverageLightPercent()).thenReturn(60.0);
    assertEquals(45.0, service.handle(new GetGlobalTelemetryQuery()).averageHumidityPercent());
    when(nodes.countByStatus(NodeStatus.ONLINE)).thenReturn(2L);
    assertNotNull(service.handle(new GetDeviceStatusMetricsQuery()));
    verify(nodes).countByStatus(NodeStatus.ONLINE);
  }
}
