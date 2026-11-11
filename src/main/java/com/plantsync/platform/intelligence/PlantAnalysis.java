package com.plantsync.platform.intelligence;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "plant_analyses",
    uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "request_key"}))
@Getter
@Setter
public class PlantAnalysis {
  @Id private String id;

  @Column(name = "owner_id", nullable = false)
  private Long ownerId;

  private Long plantId;

  @Column(name = "request_key", nullable = false, length = 100)
  private String requestKey;

  @Column(nullable = false, length = 100)
  private String fingerprint;

  @Column(nullable = false)
  private String status;

  @Lob
  @Column(columnDefinition = "LONGBLOB")
  private byte[] image;

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  private String resultJson;

  private String errorCode;
  private Instant createdAt;
  private Instant startedAt;
  @Version private Long version;
}
