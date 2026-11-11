package com.plantsync.platform.intelligence;

import com.plantsync.platform.rewards.RewardService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CapabilitiesController {
  private final GeminiClient gemini;
  private final AgentService agent;
  private final RewardService rewards;

  @org.springframework.beans.factory.annotation.Value("${integrations.iot.environment:SIMULATED}")
  private String iotEnvironment;

  @GetMapping("/api/v1/capabilities")
  public Object get() {
    return Map.of(
        "plantAnalysis",
        gemini.available(),
        "assistant",
        gemini.available(),
        "actuatorCommands",
        agent.actionsAvailable(),
        "rewards",
        rewards.available(),
        "walletChainId",
        11155111,
        "iotEnvironment",
        iotEnvironment,
        "contractVersion",
        "2026-11-10");
  }
}
