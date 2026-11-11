package com.plantsync.platform.rewards;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(
    name = "green_rewards",
    uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "plant_id"}))
@Getter
@Setter
public class Reward {
  @Id private String id;

  @Column(name = "owner_id")
  private Long ownerId;

  @Column(name = "plant_id")
  private Long plantId;

  private Long nodeId;
  private Instant startedAt;
  private Float humidityMin;
  private Float humidityMax;
  private Float lightMin;
  private String status;
  private int eligibleDays;
  private String recipient;
  private String transactionHash;

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  private String signedTransaction;

  private String errorCode;
  @Version private Long version;
}
