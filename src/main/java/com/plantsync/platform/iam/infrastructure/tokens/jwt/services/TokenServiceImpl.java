package com.plantsync.platform.iam.infrastructure.tokens.jwt.services;

import com.plantsync.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Token service implementation for JWT tokens. This class is responsible for generating and
 * validating JWT tokens. It uses the secret and expiration days from the application.properties
 * file.
 */
@Service
public class TokenServiceImpl implements BearerTokenService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);

  private static final String AUTHORIZATION_PARAMETER_NAME = "Authorization";
  private static final String BEARER_TOKEN_PREFIX = "Bearer ";

  private static final int TOKEN_BEGIN_INDEX = 7;

  @Value("${authorization.jwt.secret}")
  private String secret;

  @Value("${authorization.jwt.expiration.days}")
  private int expirationDays;

  @jakarta.annotation.PostConstruct
  void validateConfiguration() {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
    }
  }

  /**
   * This method generates a JWT token from an authentication object.
   *
   * @param authentication the authentication object
   * @return String the JWT token
   * @see Authentication
   */
  @Override
  public String generateToken(Authentication authentication) {
    return buildTokenWithDefaultParameters(authentication.getName());
  }

  /**
   * This method generates a JWT token from a email.
   *
   * @param username the email
   * @return String the JWT token
   */
  public String generateToken(String username) {
    return buildTokenWithDefaultParameters(username);
  }

  /**
   * This method generates a JWT token from an email and a secret. It uses the default expiration
   * days from the application.properties file.
   *
   * @param username the email
   * @return String the JWT token
   */
  private String buildTokenWithDefaultParameters(String username) {
    var issuedAt = new Date();
    var expiration = DateUtils.addDays(issuedAt, expirationDays);
    var key = getSigningKey();
    return Jwts.builder()
        .subject(username)
        .issuedAt(issuedAt)
        .expiration(expiration)
        .signWith(key)
        .compact();
  }

  /**
   * This method extracts the email from a JWT token.
   *
   * @param token the token
   * @return String the email
   */
  @Override
  public String getEmailFromToken(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * This method validates a JWT token.
   *
   * @param token the token
   * @return boolean true if the token is valid, false otherwise
   */
  @Override
  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);

      return true;
    } catch (SignatureException e) {
      LOGGER.debug("Token rejected");
    } catch (MalformedJwtException e) {
      LOGGER.debug("Token rejected");
    } catch (ExpiredJwtException e) {
      LOGGER.debug("Token rejected");
    } catch (UnsupportedJwtException e) {
      LOGGER.debug("Token rejected");
    } catch (IllegalArgumentException e) {
      LOGGER.debug("Token rejected");
    }
    return false;
  }

  /**
   * Extract a claim from a token.
   *
   * @param token the token
   * @param claimsResolvers the claims resolver
   * @param <T> the type of the claim
   * @return T the claim
   */
  private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
    final Claims claims = extractAllClaims(token);
    return claimsResolvers.apply(claims);
  }

  /**
   * Extract all claims from a token.
   *
   * @param token the token
   * @return Claims the claims
   */
  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  /**
   * Get the signing key.
   *
   * @return SecretKey the signing key
   */
  private SecretKey getSigningKey() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  private boolean isTokenPresentIn(String authorizationParameter) {
    return StringUtils.hasText(authorizationParameter);
  }

  private boolean isBearerTokenIn(String authorizationParameter) {
    return authorizationParameter.startsWith(BEARER_TOKEN_PREFIX);
  }

  private String extractTokenFrom(String authorizationHeaderParameter) {
    return authorizationHeaderParameter.substring(TOKEN_BEGIN_INDEX);
  }

  private String getAuthorizationParameterFrom(HttpServletRequest request) {
    return request.getHeader(AUTHORIZATION_PARAMETER_NAME);
  }

  @Override
  public String getBearerTokenFrom(HttpServletRequest request) {
    String parameter = getAuthorizationParameterFrom(request);
    if (isTokenPresentIn(parameter) && isBearerTokenIn(parameter)) {
      return extractTokenFrom(parameter);
    }
    return null;
  }
}
