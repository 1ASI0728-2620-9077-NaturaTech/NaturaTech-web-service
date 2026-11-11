package com.plantsync.platform.intelligence;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import jakarta.persistence.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AiQuotaService {
  private final EntityManager em;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void consume(Long owner) {
    em.find(
        com.plantsync.platform.iam.domain.model.aggregates.User.class,
        owner,
        LockModeType.PESSIMISTIC_WRITE);
    var quota = em.find(AiQuota.class, owner);
    if (quota == null) {
      quota = new AiQuota();
      quota.setOwnerId(owner);
      quota.setDay(LocalDate.now(ZoneOffset.UTC));
      em.persist(quota);
    }
    if (!LocalDate.now(ZoneOffset.UTC).equals(quota.getDay())) {
      quota.setRequests(0);
      quota.setDay(LocalDate.now(ZoneOffset.UTC));
    }
    if (quota.getRequests() >= 50)
      throw new ResponseStatusException(TOO_MANY_REQUESTS, "AGENT_DAILY_QUOTA");
    quota.setRequests(quota.getRequests() + 1);
  }
}
