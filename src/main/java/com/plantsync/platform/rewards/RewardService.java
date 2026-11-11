package com.plantsync.platform.rewards;

import static org.springframework.http.HttpStatus.*;

import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.*;
import com.plantsync.platform.plantprofiles.infrastructure.persistence.jpa.repositories.PlantRepository;
import com.plantsync.platform.shared.security.ResourceAccess;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RewardService {
  private final RewardRepository rewards;
  private final WalletBindingRepository wallets;
  private final PlantRepository plants;
  private final IotNodeRepository nodes;
  private final IotSensorReadingRepository readings;
  private final ResourceAccess access;
  private final SepoliaClient chain;
  private final EntityManager em;
  private final com.fasterxml.jackson.databind.ObjectMapper json;

  @Value("${integrations.rewards.rules:{}}")
  private String approvedRules;

  @Value("${integrations.rewards.enabled:false}")
  private boolean enabled;

  public boolean available() {
    return enabled && chain.available();
  }

  @Transactional
  public Reward enroll(Long plantId) {
    if (!enabled) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "REWARDS_NOT_ENABLED");
    if (!access.plant(plantId)) throw new ResponseStatusException(FORBIDDEN, "ACCESS_DENIED");
    var owner = access.currentUser();
    em.find(
        com.plantsync.platform.iam.domain.model.aggregates.User.class,
        owner.getId(),
        LockModeType.PESSIMISTIC_WRITE);
    var existing = rewards.findByOwnerIdAndPlantId(owner.getId(), plantId);
    if (existing.isPresent()) return existing.get();
    var plant = plants.findById(plantId).orElseThrow();
    var node =
        nodes
            .findByPlantIdValue(plantId)
            .orElseThrow(() -> new ResponseStatusException(CONFLICT, "PLANT_HAS_NO_DEVICE"));
    Float min, max, light;
    try {
      var rule = json.readTree(approvedRules).path(plantId.toString());
      if (!rule.path("humidityMin").isNumber()
          || !rule.path("humidityMax").isNumber()
          || !rule.path("lightMin").isNumber()) throw new IllegalArgumentException();
      min = (float) rule.path("humidityMin").asDouble();
      max = (float) rule.path("humidityMax").asDouble();
      light = (float) rule.path("lightMin").asDouble();
    } catch (Exception e) {
      throw new ResponseStatusException(CONFLICT, "REWARD_RULE_NOT_APPROVED");
    }
    if (min == null
        || max == null
        || light == null
        || !Float.isFinite(min)
        || !Float.isFinite(max)
        || !Float.isFinite(light)
        || min < 0
        || max > 100
        || min > max
        || light < 0
        || light > 100) throw new ResponseStatusException(CONFLICT, "THRESHOLDS_INVALID");
    var r = new Reward();
    r.setId(UUID.randomUUID().toString());
    r.setOwnerId(owner.getId());
    r.setPlantId(plantId);
    r.setNodeId(node.getId());
    r.setStartedAt(Instant.now());
    r.setHumidityMin(min);
    r.setHumidityMax(max);
    r.setLightMin(light);
    r.setStatus("IN_PROGRESS");
    return rewards.save(r);
  }

  public Reward own(String id) {
    var r =
        rewards
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "REWARD_NOT_FOUND"));
    if (!r.getOwnerId().equals(access.currentUser().getId()))
      throw new ResponseStatusException(NOT_FOUND, "REWARD_NOT_FOUND");
    return r;
  }

  @Transactional
  public Reward restart(String id) {
    var r = own(id);
    em.lock(r, LockModeType.PESSIMISTIC_WRITE);
    if (!"IN_PROGRESS".equals(r.getStatus()))
      throw new ResponseStatusException(CONFLICT, "REWARD_CANNOT_RESTART");
    if (!access.plant(r.getPlantId()))
      throw new ResponseStatusException(FORBIDDEN, "ACCESS_DENIED");
    var node =
        nodes
            .findByPlantIdValue(r.getPlantId())
            .orElseThrow(() -> new ResponseStatusException(CONFLICT, "PLANT_HAS_NO_DEVICE"));
    r.setNodeId(node.getId());
    r.setStartedAt(Instant.now());
    r.setEligibleDays(0);
    return r;
  }

  @Transactional
  public Reward refresh(String id) {
    var r = own(id);
    em.lock(r, LockModeType.PESSIMISTIC_WRITE);
    if (Set.of("IN_PROGRESS", "ELIGIBLE").contains(r.getStatus())) evaluate(r);
    return r;
  }

  private void evaluate(Reward r) {
    var node =
        nodes
            .findById(r.getNodeId())
            .orElseThrow(() -> new ResponseStatusException(CONFLICT, "DEVICE_MISSING"));
    if (node.getLinkedAt() != null && node.getLinkedAt().isAfter(r.getStartedAt()))
      throw new ResponseStatusException(CONFLICT, "DEVICE_LINK_CHANGED");
    if (node.getPlantId() == null
        || !node.getPlantId().value().equals(r.getPlantId())
        || !access.plant(r.getPlantId()))
      throw new ResponseStatusException(CONFLICT, "DEVICE_LINK_CHANGED");
    var end =
        Instant.now().isBefore(r.getStartedAt().plus(Duration.ofDays(30)))
            ? Instant.now()
            : r.getStartedAt().plus(Duration.ofDays(30));
    var samples =
        readings.findByNodeIdAndTimestampBetween(
            r.getNodeId(),
            LocalDateTime.ofInstant(r.getStartedAt(), ZoneOffset.UTC),
            LocalDateTime.ofInstant(end, ZoneOffset.UTC));
    r.setEligibleDays(TelemetryEligibility.days(r, samples, Instant.now()));
    r.setStatus(r.getEligibleDays() >= 30 ? "ELIGIBLE" : "IN_PROGRESS");
  }

  @Transactional
  public Reward claim(String id, String key) {
    if (key == null || !key.matches("[a-zA-Z0-9_-]{8,100}"))
      throw new ResponseStatusException(BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED");
    var r = own(id);
    em.lock(r, LockModeType.PESSIMISTIC_WRITE);
    if (Set.of("SUBMITTED", "CONFIRMED", "FAILED").contains(r.getStatus())) return r;
    if (!available())
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "SEPOLIA_NOT_CONFIGURED");
    evaluate(r);
    if (!"ELIGIBLE".equals(r.getStatus()))
      throw new ResponseStatusException(CONFLICT, "REWARD_NOT_ELIGIBLE");
    // A database-wide mint lock serializes nonce allocation across application instances.
    var lock = em.find(MintLock.class, 1L, LockModeType.PESSIMISTIC_WRITE);
    if (lock == null)
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "MINTER_NOT_INITIALIZED");
    if (!rewards.findTop20ByStatusOrderByStartedAtAsc("SUBMITTED").isEmpty())
      throw new ResponseStatusException(CONFLICT, "MINTER_BUSY");
    em.find(
        com.plantsync.platform.iam.domain.model.aggregates.User.class,
        r.getOwnerId(),
        LockModeType.PESSIMISTIC_WRITE);
    var wallet =
        wallets
            .findById(r.getOwnerId())
            .orElseThrow(() -> new ResponseStatusException(CONFLICT, "WALLET_REQUIRED"));
    try {
      var signed = chain.prepare(r.getId(), wallet.getAddress());
      r.setRecipient(wallet.getAddress());
      r.setSignedTransaction(signed.raw());
      r.setTransactionHash(signed.hash());
      r.setStatus("SUBMITTED");
      return r;
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(BAD_GATEWAY, "SEPOLIA_UNAVAILABLE");
    }
  }

  public Object list() {
    var response = new LinkedHashMap<String, Object>();
    response.put(
        "items",
        rewards.findByOwnerId(access.currentUser().getId()).stream().map(this::view).toList());
    response.put("nextCursor", null);
    return response;
  }

  public Map<String, Object> view(Reward r) {
    var result = new LinkedHashMap<String, Object>();
    result.put("id", r.getId());
    result.put("plantId", r.getPlantId());
    result.put("status", r.getStatus());
    result.put("eligibleDays", r.getEligibleDays());
    result.put("requiredDays", 30);
    result.put("chainId", WalletService.CHAIN);
    result.put("transactionHash", r.getTransactionHash());
    result.put("startedAt", r.getStartedAt());
    result.put("ruleVersion", "continuity-v1");
    result.put("errorCode", r.getErrorCode());
    return result;
  }
}
