
package com.plantsync.platform.shared.domain.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Date;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


/**
 * EntityListener for AuditableModel.
 * Entity class for auditable model
 * */

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class AuditableModel {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter
  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Date createdAt;

  @Getter
  @LastModifiedDate
  @Column(nullable = false)
  private Date updatedAt;
}

