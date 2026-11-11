package com.plantsync.platform.intelligence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.plantsync.platform.iam.domain.model.entities.Role;
import com.plantsync.platform.iam.domain.model.valueobjects.Roles;
import com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.*;
import com.plantsync.platform.rewards.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;

@SpringBootTest(properties = "integrations.iot.approved-nodes=NODE-TEST")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IntegrationSecurityTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired RoleRepository roles;
  @Autowired AnalysisService analyses;
  @Autowired PlantAnalysisRepository analysisJobs;

  @Autowired
  com.plantsync.platform.plantprofiles.infrastructure.persistence.jpa.repositories.PlantRepository
      plants;

  @Autowired
  com.plantsync.platform.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository
      profiles;

  @Autowired
  com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotNodeRepository nodes;

  @Autowired
  com.plantsync.platform.iot.infrastructure.persistence.jpa.repositories.IotSensorReadingRepository
      readings;

  @Autowired ActionProposalRepository proposals;
  @Autowired AgentDeviceController devices;
  @MockitoBean GeminiClient gemini;
  @MockitoBean SepoliaClient chain;
  @Autowired RewardService rewardService;
  @Autowired RewardRepository rewardRepository;
  @Autowired WalletBindingRepository bindings;
  @Autowired jakarta.persistence.EntityManager em;
  String a, b;
  long userA;

  @Test
  void abandonedAnalysisFailsPrivatelyAndDeletionRetainsIdempotencyReceipt() throws Exception {
    var created =
        mvc.perform(
                multipart("/api/v1/plant-analyses")
                    .file(image())
                    .header("Authorization", a)
                    .header("Idempotency-Key", "abandoned-analysis-1"))
            .andExpect(status().isAccepted())
            .andReturn();
    String id = json.readTree(created.getResponse().getContentAsString()).path("id").asText();
    mvc.perform(delete("/api/v1/plant-analyses/" + id).header("Authorization", a))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/v1/plant-analyses").header("Authorization", b))
        .andExpect(jsonPath("$.items.length()").value(0));
    mvc.perform(get("/api/v1/plant-analyses").param("limit", "51").header("Authorization", a))
        .andExpect(status().isBadRequest());
    var job = analysisJobs.findById(id).orElseThrow();
    job.setStatus("PROCESSING");
    job.setStartedAt(java.time.Instant.now().minusSeconds(181));
    analysisJobs.flush();
    analyses.expireAbandoned();
    mvc.perform(get("/api/v1/plant-analyses/" + id).header("Authorization", a))
        .andExpect(jsonPath("$.status").value("FAILED"))
        .andExpect(jsonPath("$.errorCode").value("WORKER_INTERRUPTED"));
    assertNull(job.getImage());
    mvc.perform(delete("/api/v1/plant-analyses/" + id).header("Authorization", a))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/plant-analyses/" + id).header("Authorization", a))
        .andExpect(status().isNotFound());
    mvc.perform(
            multipart("/api/v1/plant-analyses")
                .file(image())
                .header("Authorization", a)
                .header("Idempotency-Key", "abandoned-analysis-1"))
        .andExpect(status().isConflict());
    assertEquals(
        1,
        analysisJobs.countByOwnerIdAndCreatedAtAfter(
            userA, java.time.Instant.now().minusSeconds(3600)));
  }

  @Test
  void malformedProviderResultFailsWithoutRetainingPhoto() throws Exception {
    var response =
        mvc.perform(
                multipart("/api/v1/plant-analyses")
                    .file(image())
                    .header("Authorization", a)
                    .header("Idempotency-Key", "invalid-provider-1"))
            .andReturn();
    var id = json.readTree(response.getResponse().getContentAsString()).path("id").asText();
    when(gemini.generate(anyString(), anyList()))
        .thenReturn(json.readTree("{\"status\":\"SUCCEEDED\",\"species\":123}"));
    analyses.processOne();
    mvc.perform(get("/api/v1/plant-analyses/" + id).header("Authorization", a))
        .andExpect(jsonPath("$.status").value("FAILED"));
    assertNull(analysisJobs.findById(id).orElseThrow().getImage());
    analyses.processOne();
    verify(gemini, times(1)).generate(anyString(), anyList());
  }

  @Test
  void invalidAiActuatorAndDurationNeverBecomeCommands() throws Exception {
    long plant = plantWithDevice();
    var response =
        mvc.perform(post("/api/v1/agent/conversations").header("Authorization", a)).andReturn();
    var id = json.readTree(response.getResponse().getContentAsString()).path("id").asText();
    var url = "/api/v1/agent/conversations/" + id + "/messages";
    mvc.perform(
            post(url)
                .header("Authorization", a)
                .contentType("application/json")
                .content("{\"content\":\"\"}"))
        .andExpect(status().isBadRequest());
    for (var result :
        List.of(
            "{\"reply\":\"Action\",\"actuator\":\"UNSAFE\",\"durationSeconds\":5}",
            "{\"reply\":\"Water\",\"actuator\":\"WATER_PUMP\",\"durationSeconds\":11}",
            "{\"reply\":\"\"}")) {
      when(gemini.generate(anyString(), anyList())).thenReturn(json.readTree(result));
      mvc.perform(
              post(url)
                  .header("Authorization", a)
                  .contentType("application/json")
                  .content(
                      json.writeValueAsString(
                          Map.of("content", "Riega mi planta", "plantId", plant))))
          .andExpect(status().isBadGateway());
    }
    mvc.perform(get("/api/v1/agent/conversations/" + id).header("Authorization", a))
        .andExpect(jsonPath("$.messages.length()").value(0));
  }

  @Test
  void commandTimeoutDoesNotInventExecutionAndPreservesTerminalReceipt() throws Exception {
    var id = proposal(plantWithDevice());
    mvc.perform(
            post("/api/v1/actuator-command-proposals/" + id + "/confirm")
                .header("Authorization", a)
                .header("Idempotency-Key", id))
        .andExpect(status().isAccepted());
    var p = proposals.findById(id).orElseThrow();
    p.setCommandExpiresAt(java.time.Instant.now().minusSeconds(500));
    mvc.perform(get("/api/v1/actuator-commands/" + id).header("Authorization", a))
        .andExpect(jsonPath("$.status").value("EXPIRED"));
    p.setCommandStatus("SENT");
    mvc.perform(get("/api/v1/actuator-commands/" + id).header("Authorization", a))
        .andExpect(jsonPath("$.status").value("UNKNOWN"));
    p.setCommandStatus("EXECUTED");
    mvc.perform(get("/api/v1/actuator-commands/" + id).header("Authorization", a))
        .andExpect(jsonPath("$.status").value("EXECUTED"));
    mvc.perform(
            post("/api/v1/actuator-command-proposals/" + id + "/cancel").header("Authorization", a))
        .andExpect(status().isConflict());
  }

  @Test
  void rewardsRequireContinuousTelemetryAndKeepClaimsIdempotent() throws Exception {
    long plantId = plantWithDevice();
    org.springframework.test.util.ReflectionTestUtils.setField(rewardService, "enabled", true);
    org.springframework.test.util.ReflectionTestUtils.setField(
        rewardService,
        "approvedRules",
        json.writeValueAsString(
            Map.of(
                Long.toString(plantId),
                Map.of("humidityMin", 20, "humidityMax", 60, "lightMin", 10))));
    try {
      when(chain.available()).thenReturn(true);
      var response =
          mvc.perform(
                  post("/api/v1/rewards")
                      .header("Authorization", a)
                      .contentType("application/json")
                      .content(json.writeValueAsString(Map.of("plantId", plantId))))
              .andExpect(status().isCreated())
              .andReturn();
      String id = json.readTree(response.getResponse().getContentAsString()).path("id").asText();
      mvc.perform(get("/api/v1/rewards/" + id).header("Authorization", b))
          .andExpect(status().isNotFound());
      mvc.perform(
              post("/api/v1/rewards/" + id + "/claim")
                  .header("Authorization", a)
                  .header("Idempotency-Key", id))
          .andExpect(status().isConflict());
      verify(chain, never()).prepare(anyString(), anyString());
      var reward = rewardRepository.findById(id).orElseThrow();
      var start = java.time.Instant.now().minus(java.time.Duration.ofDays(30)).minusSeconds(5);
      reward.setStartedAt(start);
      var samples = new ArrayList<com.plantsync.platform.iot.domain.model.entities.SensorReading>();
      for (int i = 0; i <= 2880; i++) {
        var sample = new com.plantsync.platform.iot.domain.model.entities.SensorReading();
        sample.setNodeId(reward.getNodeId());
        sample.setTimestamp(
            java.time.LocalDateTime.ofInstant(
                start.plusSeconds(i * 900L), java.time.ZoneOffset.UTC));
        sample.setHumidityPercent(35);
        sample.setLightPercent(30);
        samples.add(sample);
      }
      readings.saveAll(samples);
      var binding = new WalletBinding();
      binding.setOwnerId(userA);
      binding.setAddress("0x" + "1".repeat(40));
      binding.setVerifiedAt(java.time.Instant.now());
      bindings.save(binding);
      em.persist(new MintLock(1L));
      when(chain.prepare(id, binding.getAddress()))
          .thenReturn(new SepoliaClient.Signed("signed-test-transaction", "0x" + "a".repeat(64)));
      mvc.perform(get("/api/v1/rewards/" + id).header("Authorization", a))
          .andExpect(jsonPath("$.status").value("ELIGIBLE"));
      for (int i = 0; i < 2; i++)
        mvc.perform(
                post("/api/v1/rewards/" + id + "/claim")
                    .header("Authorization", a)
                    .header("Idempotency-Key", id))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("SUBMITTED"));
      verify(chain, times(1)).prepare(id, binding.getAddress());
      mvc.perform(post("/api/v1/rewards/" + id + "/restart").header("Authorization", a))
          .andExpect(status().isConflict());
    } finally {
      org.springframework.test.util.ReflectionTestUtils.setField(rewardService, "enabled", false);
      org.springframework.test.util.ReflectionTestUtils.setField(
          rewardService, "approvedRules", "{}");
    }
  }

  @Test
  void relinkingNodeInvalidatesPendingProposal() throws Exception {
    var id = proposal(plantWithDevice());
    nodes.findByNodeCode("NODE-TEST").orElseThrow().setPlantId(null);
    mvc.perform(
            post("/api/v1/actuator-command-proposals/" + id + "/confirm")
                .header("Authorization", a)
                .header("Idempotency-Key", id))
        .andExpect(status().isConflict());
  }

  long plantWithDevice() {
    var profile =
        profiles
            .findByUserId(
                new com.plantsync.platform.profiles.domain.model.valueobjects.UserId(userA))
            .orElseThrow();
    var plant = new com.plantsync.platform.plantprofiles.domain.model.aggregates.Plant();
    plant.setName(
        new com.plantsync.platform.plantprofiles.domain.model.valueobjects.PlantName("Monstera"));
    plant.setProfileId(
        new com.plantsync.platform.shared.domain.model.valueobjects.ProfileId(profile.getId()));
    plant = plants.save(plant);
    var node = new com.plantsync.platform.iot.domain.model.aggregates.IoTNode();
    node.setNodeCode("NODE-TEST");
    node.setStatus(com.plantsync.platform.iot.domain.model.valueobjects.NodeStatus.ONLINE);
    node.setProfileId(plant.getProfileId());
    node.setPlantId(
        new com.plantsync.platform.shared.domain.model.valueobjects.PlantId(plant.getId()));
    node = nodes.save(node);
    var reading = new com.plantsync.platform.iot.domain.model.entities.SensorReading();
    reading.setNodeId(node.getId());
    reading.setTimestamp(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
    reading.setHumidityPercent(35);
    reading.setLightPercent(30);
    readings.save(reading);
    return plant.getId();
  }

  String proposal(long plantId) throws Exception {
    var result =
        mvc.perform(post("/api/v1/agent/conversations").header("Authorization", a)).andReturn();
    var conversation = json.readTree(result.getResponse().getContentAsString()).path("id").asText();
    when(gemini.generate(anyString(), anyList()))
        .thenReturn(
            json.readTree(
                "{\"reply\":\"Revisa la"
                    + " luz\",\"actuator\":\"GROW_LIGHT\",\"durationSeconds\":30}"));
    var response =
        mvc.perform(
                post("/api/v1/agent/conversations/" + conversation + "/messages")
                    .header("Authorization", a)
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("content", "Enciende la luz de mi planta", "plantId", plantId))))
            .andExpect(status().isOk())
            .andReturn();
    return json.readTree(response.getResponse().getContentAsString())
        .path("proposal")
        .path("id")
        .asText();
  }

  @Test
  void commandRequiresConfirmationAndRejectsDuplicateExecution() throws Exception {
    var id = proposal(plantWithDevice());
    mvc.perform(get("/api/v1/actuator-commands/" + id).header("Authorization", a))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/actuator-command-proposals/" + id + "/confirm")
                .header("Authorization", b)
                .header("Idempotency-Key", id))
        .andExpect(status().isNotFound());
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/v1/actuator-command-proposals/" + id + "/confirm")
                  .header("Authorization", a)
                  .header("Idempotency-Key", id))
          .andExpect(status().isAccepted())
          .andExpect(jsonPath("$.commandId").value(id))
          .andExpect(jsonPath("$.status").value("QUEUED"));
    String secret = "test-device-secret-with-at-least-32-characters";
    String hash =
        HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8)));
    org.springframework.test.util.ReflectionTestUtils.setField(
        devices, "keyHashes", json.writeValueAsString(Map.of("NODE-TEST", hash)));
    var url = "/api/v1/iot/edge/nodes/NODE-TEST/agent-commands";
    mvc.perform(get(url).header("Authorization", a).header("X-Node-Key", secret))
        .andExpect(status().isForbidden());
    mvc.perform(
            get(url).header("X-Edge-Api-Key", "test-only-edge-key").header("X-Node-Key", secret))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].commandId").value(id));
    mvc.perform(
            post(url + "/" + id + "/receipt")
                .header("X-Edge-Api-Key", "test-only-edge-key")
                .header("X-Node-Key", secret)
                .contentType("application/json")
                .content("{\"status\":\"EXECUTED\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/actuator-commands/" + id).header("Authorization", a))
        .andExpect(jsonPath("$.status").value("EXECUTED"));
    mvc.perform(
            post(url + "/" + id + "/receipt")
                .header("X-Edge-Api-Key", "test-only-edge-key")
                .header("X-Node-Key", secret)
                .contentType("application/json")
                .content("{\"status\":\"FAILED\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void cancelledAndExpiredProposalsCannotActivateAnything() throws Exception {
    var plantId = plantWithDevice();
    var cancelled = proposal(plantId);
    mvc.perform(
            post("/api/v1/actuator-command-proposals/" + cancelled + "/cancel")
                .header("Authorization", a))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/actuator-command-proposals/" + cancelled + "/confirm")
                .header("Authorization", a)
                .header("Idempotency-Key", cancelled))
        .andExpect(status().isConflict());
    var expired = proposal(plantId);
    proposals.findById(expired).orElseThrow().setExpiresAt(java.time.Instant.now().minusSeconds(1));
    mvc.perform(
            post("/api/v1/actuator-command-proposals/" + expired + "/confirm")
                .header("Authorization", a)
                .header("Idempotency-Key", expired))
        .andExpect(status().isConflict());
  }

  @BeforeEach
  void setup() throws Exception {
    for (var role : Roles.values()) if (!roles.existsByName(role)) roles.save(new Role(role));
    a = account();
    b = account();
    var me =
        json.readTree(
            mvc.perform(get("/api/v1/me").header("Authorization", a))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    userA = me.path("id").asLong();
    when(gemini.available()).thenReturn(true);
  }

  String account() throws Exception {
    String email = "long-valid-address-" + UUID.randomUUID() + "@test.local";
    mvc.perform(
            post("/api/v1/authentication/sign-up")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "name",
                            "Test",
                            "email",
                            email,
                            "password",
                            "test-password-123",
                            "subscriptionPlan",
                            "BASIC"))))
        .andExpect(status().isCreated());
    var response =
        mvc.perform(
                post("/api/v1/authentication/sign-in")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("email", email, "password", "test-password-123"))))
            .andExpect(status().isOk())
            .andReturn();
    return "Bearer "
        + json.readTree(response.getResponse().getContentAsString()).path("token").asText();
  }

  @Test
  void identityAndDirectoriesAreProtected() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
    mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/users").header("Authorization", a)).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/users/" + userA).header("Authorization", b))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/users/" + userA).header("Authorization", a))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/profiles/by-user-id")
                .param("userId", Long.toString(userA))
                .header("Authorization", b))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidCredentialsReturn401WithoutAccountEnumeration() throws Exception {
    mvc.perform(
            post("/api/v1/authentication/sign-in")
                .contentType("application/json")
                .content("{\"email\":\"absent@test.local\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  MockMultipartFile image() throws Exception {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", bytes);
    return new MockMultipartFile("image", "plant.png", "image/png", bytes.toByteArray());
  }

  @Test
  void imageJobsAreDurableIdempotentAndPrivate() throws Exception {
    var one =
        mvc.perform(
                multipart("/api/v1/plant-analyses")
                    .file(image())
                    .header("Authorization", a)
                    .header("Idempotency-Key", "analysis-request-1"))
            .andExpect(status().isAccepted())
            .andReturn();
    var id = json.readTree(one.getResponse().getContentAsString()).path("id").asText();
    mvc.perform(
            multipart("/api/v1/plant-analyses")
                .file(image())
                .header("Authorization", a)
                .header("Idempotency-Key", "analysis-request-1"))
        .andExpect(jsonPath("$.id").value(id));
    mvc.perform(get("/api/v1/plant-analyses/" + id).header("Authorization", b))
        .andExpect(status().isNotFound());
    when(gemini.generate(anyString(), anyList()))
        .thenReturn(
            json.readTree(
                "{\"status\":\"INCONCLUSIVE\",\"species\":null,\"summary\":\"No"
                    + " concluyente\",\"observations\":[],\"recommendations\":[]}"));
    analyses.processOne();
    mvc.perform(get("/api/v1/plant-analyses/" + id).header("Authorization", a))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INCONCLUSIVE"));
    mvc.perform(delete("/api/v1/plant-analyses/" + id).header("Authorization", a))
        .andExpect(status().isNoContent());
  }

  @Test
  void rejectsFakeImagesAndMissingProvider() throws Exception {
    mvc.perform(
            multipart("/api/v1/plant-analyses")
                .file(
                    new MockMultipartFile("image", "fake.png", "image/png", "not-image".getBytes()))
                .header("Authorization", a)
                .header("Idempotency-Key", "analysis-request-2"))
        .andExpect(status().isUnsupportedMediaType());
    when(gemini.available()).thenReturn(false);
    mvc.perform(
            multipart("/api/v1/plant-analyses")
                .file(image())
                .header("Authorization", a)
                .header("Idempotency-Key", "analysis-request-3"))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void assistantPersistsMessagesAndDeniesOtherUsers() throws Exception {
    var result =
        mvc.perform(post("/api/v1/agent/conversations").header("Authorization", a))
            .andExpect(status().isCreated())
            .andReturn();
    var id = json.readTree(result.getResponse().getContentAsString()).path("id").asText();
    when(gemini.generate(anyString(), anyList()))
        .thenReturn(
            json.readTree(
                "{\"reply\":\"¿Qué planta quieres"
                    + " cuidar?\",\"actuator\":null,\"durationSeconds\":0}"));
    mvc.perform(
            post("/api/v1/agent/conversations/" + id + "/messages")
                .header("Authorization", a)
                .contentType("application/json")
                .content("{\"content\":\"Ayúdame\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.proposal").isEmpty());
    mvc.perform(get("/api/v1/agent/conversations/" + id).header("Authorization", a))
        .andExpect(jsonPath("$.messages.length()").value(2));
    mvc.perform(get("/api/v1/agent/conversations/" + id).header("Authorization", b))
        .andExpect(status().isNotFound());
  }

  @Test
  void walletSignatureIsBoundToAccountAndSingleUse() throws Exception {
    var pair = Keys.createEcKeyPair();
    var address = "0x" + Keys.getAddress(pair);
    var c =
        mvc.perform(
                post("/api/v1/wallet/challenges")
                    .header("Authorization", a)
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(Map.of("address", address, "chainId", 11155111))))
            .andExpect(status().isOk())
            .andReturn();
    var challenge = json.readTree(c.getResponse().getContentAsString());
    var signature =
        Sign.signPrefixedMessage(
            challenge.path("message").asText().getBytes(StandardCharsets.UTF_8), pair);
    var bytes = new byte[65];
    System.arraycopy(signature.getR(), 0, bytes, 0, 32);
    System.arraycopy(signature.getS(), 0, bytes, 32, 32);
    bytes[64] = signature.getV()[0];
    var body =
        json.writeValueAsString(
            Map.of(
                "challengeId",
                challenge.path("challengeId").asText(),
                "signature",
                Numeric.toHexString(bytes)));
    mvc.perform(
            post("/api/v1/wallet/verify")
                .header("Authorization", b)
                .contentType("application/json")
                .content(body))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/v1/wallet/verify")
                .header("Authorization", a)
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.address").value(address));
    mvc.perform(
            post("/api/v1/wallet/verify")
                .header("Authorization", a)
                .contentType("application/json")
                .content(body))
        .andExpect(status().isConflict());
  }
}
