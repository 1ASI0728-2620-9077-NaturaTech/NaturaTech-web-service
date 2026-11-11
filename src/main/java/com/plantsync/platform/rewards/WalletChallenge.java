package com.plantsync.platform.rewards;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "wallet_challenges")
@Getter
@Setter
public class WalletChallenge {
  @Id private String id;
  private Long ownerId;
  private String address;

  @Column(length = 2000)
  private String message;

  private Instant expiresAt;
  private boolean consumed;
  @Version private Long version;
}
