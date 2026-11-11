package com.plantsync.platform.intelligence;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "ai_quotas")
@Getter
@Setter
public class AiQuota {
  @Id private Long ownerId;

  @Column(name = "quota_date")
  private LocalDate day;

  private int requests;
}
