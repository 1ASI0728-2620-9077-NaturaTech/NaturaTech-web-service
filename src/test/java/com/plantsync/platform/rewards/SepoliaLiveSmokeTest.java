package com.plantsync.platform.rewards;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantsync.platform.iam.domain.model.aggregates.User;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.UserRepository;
import com.plantsync.platform.iot.domain.model.aggregates.IoTNode;
import com.plantsync.platform.iot.domain.model.entities.SensorReading;
import com.plantsync.platform.iot.domain.model.valueobjects.NodeStatus;
import com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.*;
import com.plantsync.platform.plantprofiles.domain.model.aggregates.Plant;
import com.plantsync.platform.plantprofiles.domain.model.valueobjects.PlantName;
import com.plantsync.platform.plantprofiles.infrastructure.persistence.jpa.repositories.PlantRepository;
import com.plantsync.platform.shared.domain.model.valueobjects.PlantId;
import com.plantsync.platform.shared.security.ResourceAccess;
import jakarta.persistence.EntityManager;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

/** Explicit testnet-only exercise. Historical telemetry and wallet binding are LOCAL fixtures.
 * Never changes Render data. Default is signing/eligibility only; no network broadcast.
 * Real broadcasting additionally requires -Dsepolia.broadcast=true.
 */
@EnabledIfSystemProperty(named = "sepolia.live", matches = "true")
@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sepolia_live_smoke;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "integrations.rewards.enabled=true", "integrations.workers.enabled=false",
    "logging.level.root=WARN", "logging.level.com.plantsync.platform=WARN"})
class SepoliaLiveSmokeTest {
  @Autowired UserRepository users;
  @Autowired PlantRepository plants;
  @Autowired IotNodeRepository nodes;
  @Autowired IotSensorReadingRepository readings;
  @Autowired WalletBindingRepository bindings;
  @Autowired RewardRepository rewards;
  @Autowired RewardService service;
  @Autowired RewardWorker worker;
  @Autowired SepoliaClient chain;
  @Autowired TransactionTemplate transactions;
  @Autowired EntityManager em;
  @Autowired ObjectMapper json;
  @MockitoBean ResourceAccess access;

  @DynamicPropertySource
  static void configuration(DynamicPropertyRegistry registry) throws Exception {
    var config = new Properties();
    try (var reader = Files.newBufferedReader(Path.of(".env.local"))) { config.load(reader); }
    for (var entry : Map.of("rpc", "SEPOLIA_RPC_URL", "contract", "SEPOLIA_CONTRACT", "minter-key", "SEPOLIA_MINTER_KEY").entrySet()) {
      String value = System.getenv().getOrDefault(entry.getValue(), config.getProperty(entry.getValue(), ""));
      registry.add("integrations.sepolia." + entry.getKey(), () -> value);
    }
  }

  @Test
  void evaluatesLocalHistoryAndPreparesOrBroadcastsOneDemoBadge() throws Exception {
    String recipient = System.getProperty("sepolia.recipient", "").toLowerCase(Locale.ROOT);
    assertTrue(recipient.matches("0x[0-9a-f]{40}"), "Specify the verified PUBLIC recipient address");
    assertTrue(chain.available(), "Configure local Sepolia credentials first");
    Path destination = Path.of(".data/sepolia-live-smoke.json");
    if (Files.exists(destination)) {
      var previous = json.readTree(Files.readString(destination));
      assertFalse(previous.path("broadcastRequested").asBoolean(),
          "A live broadcast record already exists. Inspect/reconcile its hash; never repeat this test to replace a pending transaction.");
    }
    var user = users.saveAndFlush(new User("demo-" + UUID.randomUUID() + "@example.test", "unused-local-fixture"));
    when(access.currentUser()).thenReturn(user);
    when(access.plant(anyLong())).thenReturn(true);
    var plant = new Plant();
    plant.setName(new PlantName("DEMO: local synthetic telemetry"));
    plant = plants.saveAndFlush(plant);
    var node = new IoTNode();
    node.setNodeCode("DEMO-" + UUID.randomUUID());
    node.setStatus(NodeStatus.ONLINE);
    node.setPlantId(new PlantId(plant.getId()));
    nodes.saveAndFlush(node);
    ReflectionTestUtils.setField(service, "approvedRules", json.writeValueAsString(Map.of(
        plant.getId().toString(), Map.of("humidityMin", 40, "humidityMax", 70, "lightMin", 30))));
    var reward = service.enroll(plant.getId());
    String id = reward.getId();
    var start = Instant.now().minus(Duration.ofDays(30)).minusSeconds(5);
    transactions.executeWithoutResult(tx -> {
      var local = em.find(Reward.class, id);
      local.setStartedAt(start);
      var samples = new ArrayList<SensorReading>();
      for (int index = 0; index <= 2880; index++) {
        var sample = new SensorReading();
        sample.setNodeId(node.getId());
        sample.setTimestamp(LocalDateTime.ofInstant(start.plusSeconds(index * 900L), ZoneOffset.UTC));
        sample.setHumidityPercent(45);
        sample.setLightPercent(40);
        samples.add(sample);
      }
      readings.saveAll(samples);
      var binding = new WalletBinding();
      binding.setOwnerId(user.getId());
      binding.setAddress(recipient);
      binding.setVerifiedAt(Instant.now());
      bindings.save(binding);
    });
    assertEquals("ELIGIBLE", service.refresh(id).getStatus());
    worker.work(); // Initialize nonce lock; nothing has been submitted yet.
    var signed = service.claim(id, "demo-" + id);
    assertEquals("SUBMITTED", signed.getStatus());
    assertEquals(signed.getTransactionHash(), service.claim(id, "demo-" + id).getTransactionHash());
    var raw = org.web3j.crypto.TransactionDecoder.decode(signed.getSignedTransaction());
    var maxCost = raw.getGasPrice().multiply(raw.getGasLimit());
    var authorizedCap = new java.math.BigInteger(System.getProperty("sepolia.max-fee-wei", "1000000000000000"));
    assertTrue(authorizedCap.signum() > 0 && authorizedCap.compareTo(new java.math.BigInteger("1000000000000000")) <= 0);
    assertTrue(maxCost.compareTo(authorizedCap) <= 0,
        "Maximum fee exceeds 0.001 Sepolia ETH; no broadcast attempted");
    boolean broadcast = Boolean.getBoolean("sepolia.broadcast");
    var report = new LinkedHashMap<String, Object>();
    report.put("purpose", "DEMO: synthetic historical telemetry in isolated local H2, not a Render achievement");
    report.put("chainId", WalletService.CHAIN);
    report.put("rewardId", id);
    report.put("recipient", recipient);
    report.put("transactionHash", signed.getTransactionHash());
    report.put("maximumFeeWei", maxCost.toString());
    report.put("broadcastRequested", broadcast);
    Files.createDirectories(destination.getParent());
    if (broadcast) {
      // Keep the exact transaction for recovery after a process interruption.
      Files.writeString(Path.of(".data/sepolia-live-smoke-signed.txt"), signed.getSignedTransaction());
    }
    Files.writeString(destination, json.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    System.out.println("DEMO prepared: " + signed.getTransactionHash() + "; maxFeeWei=" + maxCost + "; broadcast=" + broadcast);
    if (!broadcast) return;
    // RewardWorker uses the actual SepoliaClient and validates 12 canonical confirmations
    // and the mint Transfer event to this recipient before recording CONFIRMED.
    for (int attempt = 0; attempt < 32; attempt++) {
      worker.work();
      var current = rewards.findById(id).orElseThrow();
      System.out.println("DEMO reconciliation " + attempt + ": " + current.getStatus());
      report.put("status", current.getStatus());
      Files.writeString(destination, json.writerWithDefaultPrettyPrinter().writeValueAsString(report));
      if ("CONFIRMED".equals(current.getStatus())) return;
      assertNotEquals("FAILED", current.getStatus(), "Testnet transaction failed; inspect recorded hash");
      Thread.sleep(15000);
    }
    fail("Transaction still pending; reconcile the recorded hash before any repeat broadcast");
  }
}
