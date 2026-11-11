package com.plantsync.platform.analytics.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.plantsync.platform.analytics.domain.model.queries.GetDeviceStatusMetricsQuery;
import com.plantsync.platform.analytics.domain.model.queries.GetGlobalTelemetryQuery;
import com.plantsync.platform.analytics.domain.services.AnalyticsQueryService;
import com.plantsync.platform.analytics.interfaces.rest.assemblers.DeviceStatusMetricsResourceFromEntityAssembler;
import com.plantsync.platform.analytics.interfaces.rest.assemblers.GlobalTelemetryResourceFromEntityAssembler;
import com.plantsync.platform.analytics.interfaces.rest.resources.DeviceStatusMetricsResource;
import com.plantsync.platform.analytics.interfaces.rest.resources.GlobalTelemetryResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/analytics", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Analytics", description = "Admin Analytics Endpoints")
public class AdminAnalyticsController {

  private final AnalyticsQueryService analyticsQueryService;

  public AdminAnalyticsController(AnalyticsQueryService analyticsQueryService) {
    this.analyticsQueryService = analyticsQueryService;
  }

  @GetMapping("/global-telemetry")
  @Operation(summary = "Get global telemetry", description = "Get aggregated global telemetry data (average light, air temperature, gas and humidity)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Global telemetry data retrieved"),
      @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")})
  public ResponseEntity<GlobalTelemetryResource> getGlobalTelemetry() {
    var query = new GetGlobalTelemetryQuery();
    var telemetry = analyticsQueryService.handle(query);
    var resource = GlobalTelemetryResourceFromEntityAssembler.toResourceFromEntity(telemetry);
    return ResponseEntity.ok(resource);
  }

  @GetMapping("/device-status")
  @Operation(summary = "Get device status metrics", description = "Get aggregated device status metrics (online, offline, error counts)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Device status metrics retrieved"),
      @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")})
  public ResponseEntity<DeviceStatusMetricsResource> getDeviceStatus() {
    var query = new GetDeviceStatusMetricsQuery();
    var metrics = analyticsQueryService.handle(query);
    var resource = DeviceStatusMetricsResourceFromEntityAssembler.toResourceFromEntity(metrics);
    return ResponseEntity.ok(resource);
  }
}
