package com.plantsync.platform.rewards;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRepository extends JpaRepository<Reward, String> {
  Optional<Reward> findByOwnerIdAndPlantId(Long ownerId, Long plantId);

  List<Reward> findByOwnerId(Long ownerId);

  List<Reward> findTop20ByStatusOrderByStartedAtAsc(String status);
}
