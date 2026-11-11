package com.plantsync.platform.rewards;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class RewardWorker {
  private final RewardRepository rewards;
  private final SepoliaClient chain;
  private final EntityManager em;
  private final TransactionTemplate transactions;

  @org.springframework.beans.factory.annotation.Value("${integrations.rewards.enabled:false}")
  private boolean enabled;

  @Scheduled(fixedDelayString = "${integrations.rewards.poll-ms:15000}")
  public void work() {
    if (!enabled || !chain.available()) return;
    transactions.executeWithoutResult(
        tx -> {
          if (em.find(MintLock.class, 1L) == null) em.persist(new MintLock(1L));
        });
    for (var reward : rewards.findTop20ByStatusOrderByStartedAtAsc("SUBMITTED")) {
      try {
        var state = chain.reconcile(reward);
        transactions.executeWithoutResult(
            tx -> {
              var r = em.find(Reward.class, reward.getId(), LockModeType.PESSIMISTIC_WRITE);
              if ("SUBMITTED".equals(r.getStatus())) {
                r.setStatus(state);
                if ("FAILED".equals(state)) r.setErrorCode("TRANSACTION_REVERTED");
              }
            });
      } catch (Exception e) {
        /* Keep durable transaction for reconciliation. Never mint a replacement. */
        log.warn(
            "Reward reconciliation deferred for {} ({})",
            reward.getId(),
            e.getClass().getSimpleName());
      }
    }
  }
}
