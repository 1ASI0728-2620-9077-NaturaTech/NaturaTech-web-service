package com.plantsync.platform.shared.domain.model.aggregates;

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
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


/**
 * Base class for all aggregate roots that require auditing.
 *
 * @param <T> the type of the aggregate root
 */
@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class AuditableAbstractAggregateRoot<T extends AbstractAggregateRoot<T>>
    extends AbstractAggregateRoot<T> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Date createdAt;
  @LastModifiedDate
  @Column(nullable = false)
  private Date updatedAt;

  /**
   * Registers a domain event.
   *
   * @param event the domain event to register
   */
  public void addDomainEvent(Object event) {
    super.registerEvent(event);
  }
}