package com.plantsync.platform.intelligence;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantAnalysisRepository extends JpaRepository<PlantAnalysis, String> {
  Optional<PlantAnalysis> findByOwnerIdAndRequestKey(Long ownerId, String requestKey);

  @org.springframework.data.jpa.repository.Query(
      "select a from PlantAnalysis a where a.ownerId = :ownerId and a.status <> 'DELETED' order by"
          + " a.createdAt desc")
  List<PlantAnalysis> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable page);

  @org.springframework.data.jpa.repository.Query(
      "select a from PlantAnalysis a where a.ownerId = :ownerId and a.plantId = :plantId and"
          + " a.status <> 'DELETED' order by a.createdAt desc")
  List<PlantAnalysis> findByOwnerIdAndPlantIdOrderByCreatedAtDesc(
      Long ownerId, Long plantId, Pageable page);

  List<PlantAnalysis> findByStatusOrderByCreatedAtAsc(String status, Pageable page);

  long countByOwnerIdAndCreatedAtAfter(Long ownerId, Instant since);

  List<PlantAnalysis> findByStatusAndStartedAtBefore(String status, Instant before);
}
