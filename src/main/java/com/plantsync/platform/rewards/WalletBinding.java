package com.plantsync.platform.rewards;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "wallet_bindings")
@Getter
@Setter
public class WalletBinding {
  @Id private Long ownerId;

  @Column(unique = true, nullable = false)
  private String address;

  private Instant verifiedAt;
  @Version private Long version;
}
