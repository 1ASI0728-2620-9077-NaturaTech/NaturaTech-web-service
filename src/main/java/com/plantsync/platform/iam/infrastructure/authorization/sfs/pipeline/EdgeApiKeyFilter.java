package com.plantsync.platform.iam.infrastructure.authorization.sfs.pipeline;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class EdgeApiKeyFilter extends OncePerRequestFilter {

  private static final String EDGE_PATH_PREFIX = "/api/v1/iot/edge/";
  private static final String EDGE_API_KEY_HEADER = "X-Edge-Api-Key";

  private final String edgeApiKey;

  public EdgeApiKeyFilter(@Value("${edge.api.key:}") String edgeApiKey) {
    this.edgeApiKey = edgeApiKey;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request
        .getRequestURI()
        .substring(request.getContextPath().length())
        .startsWith(EDGE_PATH_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    var apiKey = request.getHeader(EDGE_API_KEY_HEADER);
    if (!edgeApiKey.isBlank()
        && apiKey != null
        && java.security.MessageDigest.isEqual(
            edgeApiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            apiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
      var authentication =
          new UsernamePasswordAuthenticationToken(
              "edge-service", null, List.of(new SimpleGrantedAuthority("ROLE_EDGE_SERVICE")));
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    filterChain.doFilter(request, response);
  }
}
