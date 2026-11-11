package com.plantsync.platform.intelligence;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionProposalRepository extends JpaRepository<ActionProposal, String> {
  long countByNodeIdAndConfirmedAtAfter(Long nodeId, Instant after);

  List<ActionProposal> findByNodeCodeAndCommandStatusIn(
      String nodeCode, Collection<String> statuses);
}
