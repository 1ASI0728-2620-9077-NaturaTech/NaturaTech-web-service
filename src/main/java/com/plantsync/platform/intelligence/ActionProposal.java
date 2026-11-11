package com.plantsync.platform.intelligence;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "agent_action_proposals")
@Getter
@Setter
public class ActionProposal {
  @Id private String id;

  @Column(nullable = false)
  private Long ownerId;

  private String conversationId;
  private Long plantId;
  private Long nodeId;
  private String nodeCode;
  private String actuator;
  private int durationSeconds;
  private String status;
  private String commandStatus;
  private Instant createdAt;
  private Instant expiresAt;
  private Instant confirmedAt;
  private Instant commandExpiresAt;
  @Version private Long version;
}
