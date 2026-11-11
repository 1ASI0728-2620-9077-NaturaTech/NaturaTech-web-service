package com.plantsync.platform.rewards;

import jakarta.persistence.*;

@Entity
@Table(name = "mint_locks")
public class MintLock {
  @Id private Long id;

  public MintLock() {}

  public MintLock(Long id) {
    this.id = id;
  }
}
