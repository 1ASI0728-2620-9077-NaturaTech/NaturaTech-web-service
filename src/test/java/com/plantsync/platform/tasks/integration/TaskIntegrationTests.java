package com.plantsync.platform.tasks.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantsync.platform.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import com.plantsync.platform.tasks.interfaces.rest.resources.CreateTaskResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaskIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TaskRepository taskRepository;

  @Autowired
  com.plantsync.platform.iam.infrastructure.persistence.jpa.respositories.UserRepository users;

  @Autowired
  com.plantsync.platform.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository
      profiles;

  @Autowired
  com.plantsync.platform.plantprofiles.infrastructure.persistence.jpa.repositories.PlantRepository
      plants;

  private Long profileId;
  private Long plantId;

  @BeforeEach
  void setUp() {
    taskRepository.deleteAll();
    var user =
        users
            .findByEmail("user")
            .orElseGet(
                () ->
                    users.save(
                        new com.plantsync.platform.iam.domain.model.aggregates.User(
                            "user", "test-hash")));
    var profile =
        profiles
            .findByUserId(
                new com.plantsync.platform.profiles.domain.model.valueobjects.UserId(user.getId()))
            .orElseGet(
                () -> {
                  var p = new com.plantsync.platform.profiles.domain.model.aggregates.Profile();
                  p.setUserId(
                      new com.plantsync.platform.profiles.domain.model.valueobjects.UserId(
                          user.getId()));
                  p.setPersonName(
                      new com.plantsync.platform.profiles.domain.model.valueobjects.PersonName(
                          "Test User"));
                  p.setSubscriptionPlan(
                      com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan
                          .BASIC);
                  return profiles.save(p);
                });
    profileId = profile.getId();
    var plant = new com.plantsync.platform.plantprofiles.domain.model.aggregates.Plant();
    plant.setName(
        new com.plantsync.platform.plantprofiles.domain.model.valueobjects.PlantName("Monstera"));
    plant.setProfileId(
        new com.plantsync.platform.shared.domain.model.valueobjects.ProfileId(profileId));
    plantId = plants.save(plant).getId();
  }

  @Test
  @WithMockUser
  public void createTask_ShouldReturnCreatedAndPersistInDatabase() throws Exception {
    CreateTaskResource resource =
        new CreateTaskResource("Watering", "2025-07-03", plantId, profileId, null, null);

    mockMvc
        .perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resource)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.action").value("Watering"))
        .andExpect(jsonPath("$.status").value("PENDING"));

    assertThat(taskRepository.count()).isEqualTo(1);
    var task = taskRepository.findAll().get(0);
    assertThat(task.getAction()).isEqualTo("Watering");
    assertThat(task.getCompletedAt()).isNull();
  }

  @Test
  @WithMockUser
  public void getAllTasks_ShouldReturnTasksList() throws Exception {
    CreateTaskResource resource =
        new CreateTaskResource("Fertilizing", "2025-08-03", plantId, profileId, null, null);

    mockMvc
        .perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resource)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/tasks").param("plantId", plantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].action").value("Fertilizing"))
        .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  @Test
  @WithMockUser
  public void completeTask_ShouldMarkTaskAsCompleted() throws Exception {
    CreateTaskResource resource =
        new CreateTaskResource("Watering", "2025-07-03", plantId, profileId, null, null);

    var result =
        mockMvc
            .perform(
                post("/api/v1/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resource)))
            .andExpect(status().isCreated())
            .andReturn();
    var taskId =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

    mockMvc
        .perform(
            patch("/api/v1/tasks/" + taskId + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new com.plantsync.platform.tasks.interfaces.rest.resources
                            .CompleteTaskResource(80, "Done"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.humidity").value(80));

    var task = taskRepository.findById(taskId);
    assertThat(task).isPresent();
    assertThat(task.get().getCompletedAt()).isNotNull();
  }
}
