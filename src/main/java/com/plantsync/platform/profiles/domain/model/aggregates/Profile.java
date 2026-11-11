package com.plantsync.platform.profiles.domain.model.aggregates;

import com.plantsync.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.plantsync.platform.profiles.domain.model.valueobjects.PaymentStatus;
import com.plantsync.platform.profiles.domain.model.valueobjects.PersonName;
import com.plantsync.platform.profiles.domain.model.valueobjects.SubscriptionPlan;
import com.plantsync.platform.profiles.domain.model.valueobjects.UserId;
import com.plantsync.platform.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

/**
 * Aggregate root representing a user profile in the system.
 * A profile is linked to a user and contains subscription and payment status
 * information.
 */
@Getter
@Setter
@Entity
public class Profile extends AuditableAbstractAggregateRoot<Profile> {

  /**
   * The full name of the person associated with the profile.
   */
  @Embedded
  private PersonName personName;

  /**
   * The user's profile picture encoded as Base64.
   */
  @Lob
  @Column(columnDefinition = "TEXT")
  private String profilePictureBase64;

  /**
   * The current subscription plan of the user (e.g., FREE, PRO).
   */
  @Enumerated(EnumType.STRING)
  private SubscriptionPlan subscriptionPlan;

  /**
   * The ID of the user who owns this profile.
   */
  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "user_id"))
  private UserId userId;

  /**
   * The current payment status of the subscription (e.g., PENDING, PAID).
   */
  @Enumerated(EnumType.STRING)
  private PaymentStatus paymentStatus = PaymentStatus.PENDING;

  /**
   * Default constructor required by JPA.
   */
  public Profile() {
    super();
  }

  /**
   * Creates a new profile from the given {@link CreateProfileCommand}.
   *
   * @param command the command with all necessary fields to create a profile
   */
  public Profile(CreateProfileCommand command) {
    this.personName = command.personName();
    this.subscriptionPlan = command.subscriptionPlan();
    this.userId = command.userId();
    this.paymentStatus = PaymentStatus.PENDING;
  }

  /**
   * Constructs a profile with explicit values, used for internal instantiation or
   * testing.
   *
   * @param name             the person's name
   * @param subscriptionPlan the user's subscription plan
   * @param userId           the associated user ID
   * @param paymentStatus    the current payment status
   */
  public Profile(PersonName name, SubscriptionPlan subscriptionPlan, UserId userId,
                 PaymentStatus paymentStatus) {
    this.personName = name;
    this.subscriptionPlan = subscriptionPlan;
    this.userId = userId;
    this.paymentStatus = paymentStatus;
  }

  /**
   * Update information profile.
   *
   * @param newPersonName       the new person name
   * @param newSubscriptionPlan the new subscription plan
   * @param newProfilePictureBase64 the new profile picture encoded as Base64
   * @return the profile
   */
  public Profile updateInformation(PersonName newPersonName, SubscriptionPlan newSubscriptionPlan,
                                   String newProfilePictureBase64) {
    this.personName = newPersonName;
    this.subscriptionPlan = newSubscriptionPlan;
    this.profilePictureBase64 = newProfilePictureBase64;
    return this;
  }

}
