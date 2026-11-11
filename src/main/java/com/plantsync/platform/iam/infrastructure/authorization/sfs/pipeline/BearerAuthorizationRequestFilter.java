package com.plantsync.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.plantsync.platform.iam.infrastructure.authorization.sfs.model.EmailPasswordAuthenticationTokenBuilder;
import com.plantsync.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer Authorization Request Filter.
 *
 * <p>This class is responsible for filtering requests and setting the user authentication. It
 * extends the OncePerRequestFilter class.
 *
 * @see OncePerRequestFilter
 */
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(BearerAuthorizationRequestFilter.class);
  private final BearerTokenService tokenService;

  @Qualifier("defaultUserDetailsService")
  private final UserDetailsService userDetailsService;

  /**
   * Constructor for BearerAuthorizationRequestFilter.
   *
   * @param tokenService The token service.
   * @param userDetailsService The user details service.
   */
  public BearerAuthorizationRequestFilter(
      BearerTokenService tokenService, UserDetailsService userDetailsService) {
    this.tokenService = tokenService;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    boolean skip =
        path.startsWith("/api/v1/authentication/")
            || path.startsWith("/v3/api-docs/")
            || path.startsWith("/swagger-ui/")
            || path.equals("/swagger-ui.html")
            || path.equals("/");

    return skip;
  }

  /**
   * This method is responsible for filtering requests and setting the user authentication.
   *
   * @param request The request object.
   * @param response The response object.
   * @param filterChain The filter chain object.
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String token = tokenService.getBearerTokenFrom(request);

      if (token != null && tokenService.validateToken(token)) {
        String email = tokenService.getEmailFromToken(token);
        var userDetails = userDetailsService.loadUserByUsername(email);
        SecurityContextHolder.getContext()
            .setAuthentication(EmailPasswordAuthenticationTokenBuilder.build(userDetails, request));
      } else {
        LOGGER.debug("No valid bearer token");
      }

    } catch (Exception e) {
      LOGGER.debug("Authentication rejected");
    }
    filterChain.doFilter(request, response);
  }
}
