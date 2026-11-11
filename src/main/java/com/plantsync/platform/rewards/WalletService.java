package com.plantsync.platform.rewards;

import static org.springframework.http.HttpStatus.*;

import com.plantsync.platform.shared.security.ResourceAccess;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;

@Service
@RequiredArgsConstructor
public class WalletService {
  private final ResourceAccess access;
  private final WalletChallengeRepository challenges;
  private final WalletBindingRepository bindings;
  private final EntityManager em;

  @Value("${integrations.wallet.domain:localhost:5173}")
  private String domain;

  public static final long CHAIN = 11155111;

  @Transactional
  public Map<String, Object> challenge(String address, long chain) {
    if (chain != CHAIN || address == null || !address.matches("0x[0-9a-fA-F]{40}"))
      throw new ResponseStatusException(BAD_REQUEST, "WALLET_INVALID");
    var c = new WalletChallenge();
    c.setId(UUID.randomUUID().toString());
    c.setOwnerId(access.currentUser().getId());
    c.setAddress(address.toLowerCase(Locale.ROOT));
    c.setExpiresAt(Instant.now().plusSeconds(300));
    c.setMessage(
        domain
            + " requests verification of your PlantSync wallet.\nAddress: "
            + address
            + "\nChain ID: "
            + CHAIN
            + "\nNonce: "
            + c.getId()
            + "\nAccount: "
            + c.getOwnerId()
            + "\nExpires: "
            + c.getExpiresAt()
            + "\nThis signature does not send a transaction.");
    challenges.save(c);
    return Map.of(
        "challengeId",
        c.getId(),
        "message",
        c.getMessage(),
        "expiresAt",
        c.getExpiresAt(),
        "chainId",
        CHAIN);
  }

  @Transactional
  public WalletBinding verify(String id, String signature) {
    var user = access.currentUser();
    em.find(
        com.plantsync.platform.iam.domain.model.aggregates.User.class,
        user.getId(),
        LockModeType.PESSIMISTIC_WRITE);
    var c = em.find(WalletChallenge.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (c == null
        || !c.getOwnerId().equals(user.getId())
        || c.isConsumed()
        || !c.getExpiresAt().isAfter(Instant.now()))
      throw new ResponseStatusException(CONFLICT, "CHALLENGE_INVALID");
    if (!recover(c.getMessage(), signature).equals(c.getAddress()))
      throw new ResponseStatusException(BAD_REQUEST, "SIGNATURE_INVALID");
    c.setConsumed(true);
    var binding = bindings.findById(user.getId()).orElseGet(WalletBinding::new);
    binding.setOwnerId(user.getId());
    binding.setAddress(c.getAddress());
    binding.setVerifiedAt(Instant.now());
    return bindings.saveAndFlush(binding);
  }

  public static String recover(String message, String signature) {
    try {
      if (signature == null || !signature.matches("0x[0-9a-fA-F]{130}"))
        throw new IllegalArgumentException();
      byte[] bytes = Numeric.hexStringToByteArray(signature);
      byte v = bytes[64];
      if (v < 27) v += 27;
      var sig =
          new Sign.SignatureData(
              v, Arrays.copyOfRange(bytes, 0, 32), Arrays.copyOfRange(bytes, 32, 64));
      return "0x"
          + Keys.getAddress(
              Sign.signedPrefixedMessageToKey(message.getBytes(StandardCharsets.UTF_8), sig));
    } catch (Exception e) {
      throw new ResponseStatusException(BAD_REQUEST, "SIGNATURE_INVALID");
    }
  }

  public WalletBinding get() {
    return bindings.findById(access.currentUser().getId()).orElse(null);
  }

  @Transactional
  public void unlink() {
    var user = access.currentUser();
    em.find(
        com.plantsync.platform.iam.domain.model.aggregates.User.class,
        user.getId(),
        LockModeType.PESSIMISTIC_WRITE);
    bindings.deleteById(
        user.getId()); // Already submitted rewards retain their immutable recipient.
  }

  public Map<String, Object> view(WalletBinding b) {
    return Map.of("address", b.getAddress(), "chainId", CHAIN, "verifiedAt", b.getVerifiedAt());
  }
}
