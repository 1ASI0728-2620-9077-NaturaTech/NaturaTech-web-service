package com.plantsync.platform.intelligence;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "agent_conversations")
@Getter
@Setter
public class Conversation {
  @Id private String id;

  @Column(nullable = false)
  private Long ownerId;

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  private String messagesJson = "[]";

  private Instant createdAt;
  @Version private Long version;
}
