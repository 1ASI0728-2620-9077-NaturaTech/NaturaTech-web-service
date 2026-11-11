package com.plantsync.platform.intelligence;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "integrations.workers.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AnalysisWorker {
  private final AnalysisService service;

  @Scheduled(fixedDelayString = "${integrations.analysis.poll-ms:5000}")
  public void work() {
    service.expireAbandoned();
    service.processOne();
  }
}
