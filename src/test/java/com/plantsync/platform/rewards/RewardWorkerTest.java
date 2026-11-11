package com.plantsync.platform.rewards;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class RewardWorkerTest {
  @Test
  @SuppressWarnings("unchecked")
  void reconcilesDurableTransactionsAndRetainsFailuresForRetry() throws Exception {
    var repo = mock(RewardRepository.class);
    var chain = mock(SepoliaClient.class);
    var em = mock(EntityManager.class);
    var tx = mock(TransactionTemplate.class);
    doAnswer(
            call -> {
              ((Consumer<TransactionStatus>) call.getArgument(0))
                  .accept(mock(TransactionStatus.class));
              return null;
            })
        .when(tx)
        .executeWithoutResult(any());
    var worker = new RewardWorker(repo, chain, em, tx);
    worker.work();
    verifyNoInteractions(chain, repo);
    ReflectionTestUtils.setField(worker, "enabled", true);
    worker.work();
    verifyNoInteractions(repo);
    when(chain.available()).thenReturn(true);
    var reward = new Reward();
    reward.setId("reward");
    reward.setStatus("SUBMITTED");
    when(repo.findTop20ByStatusOrderByStartedAtAsc("SUBMITTED")).thenReturn(List.of(reward));
    when(em.find(Reward.class, "reward", LockModeType.PESSIMISTIC_WRITE)).thenReturn(reward);
    when(chain.reconcile(reward)).thenThrow(new IllegalStateException("provider down"));
    worker.work();
    assertEquals("SUBMITTED", reward.getStatus());
    doReturn("CONFIRMED").when(chain).reconcile(reward);
    worker.work();
    assertEquals("CONFIRMED", reward.getStatus());
    reward.setStatus("SUBMITTED");
    when(chain.reconcile(reward)).thenReturn("FAILED");
    worker.work();
    assertEquals("FAILED", reward.getStatus());
    assertEquals("TRANSACTION_REVERTED", reward.getErrorCode());
    when(chain.reconcile(reward)).thenReturn("CONFIRMED");
    worker.work();
    assertEquals("FAILED", reward.getStatus());
  }
}
