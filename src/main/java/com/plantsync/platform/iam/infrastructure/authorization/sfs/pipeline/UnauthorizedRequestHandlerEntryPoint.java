package com.plantsync.platform.iam.infrastructure.authorization.sfs.pipeline;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Unauthorized Request Handler.
 *
 * <p>This class is responsible for handling unauthorized requests. It is used by the Spring
 * Security framework to handle unauthorized requests. It implements the {@link
 * AuthenticationEntryPoint} interface.
 *
 * @see AuthenticationEntryPoint
 */
@Component
public class UnauthorizedRequestHandlerEntryPoint implements AuthenticationEntryPoint {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(UnauthorizedRequestHandlerEntryPoint.class);

  /**
   * This method is called by the Spring Security framework when an unauthorized request is
   * detected.
   *
   * @param request The request that caused the exception
   * @param response The response that will be sent to the client
   * @param authenticationException The exception that caused the invocation
   */
  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
    response
        .getWriter()
        .write("{\"code\":\"INVALID_TOKEN\",\"message\":\"Inicia sesión para continuar.\"}");
  }
}
