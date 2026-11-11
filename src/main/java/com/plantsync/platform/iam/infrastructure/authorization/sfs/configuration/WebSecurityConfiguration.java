package com.plantsync.platform.iam.infrastructure.authorization.sfs.configuration;

import com.plantsync.platform.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import com.plantsync.platform.iam.infrastructure.authorization.sfs.pipeline.EdgeApiKeyFilter;
import com.plantsync.platform.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import com.plantsync.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Web Security Configuration.
 *
 * <p>This class is responsible for configuring the web security. It enables the method security and
 * configures the security filter chain. It includes the authentication manager, the authentication
 * provider, the password encoder and the authentication entry point.
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {

  private final UserDetailsService userDetailsService;

  private final BearerTokenService tokenService;

  private final BCryptHashingService hashingService;

  private final AuthenticationEntryPoint unauthorizedRequestHandler;

  private final EdgeApiKeyFilter edgeApiKeyFilter;

  @org.springframework.beans.factory.annotation.Value(
      "${app.cors.origins:http://localhost:5173,http://127.0.0.1:5173}")
  private String allowedOrigins;

  /**
   * This method creates the Bearer Authorization Request Filter.
   *
   * @return The Bearer Authorization Request Filter
   * @see BearerAuthorizationRequestFilter
   */
  @Bean
  public BearerAuthorizationRequestFilter authorizationRequestFilter() {
    return new BearerAuthorizationRequestFilter(tokenService, userDetailsService);
  }

  /**
   * This method creates the authentication manager.
   *
   * @param authenticationConfiguration The {@link AuthenticationConfiguration} object with the
   *     authentication configuration.
   * @return The {@link AuthenticationManager} instance from the authentication configuration.
   */
  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  /**
   * This method creates the authentication provider.
   *
   * @return The {@link DaoAuthenticationProvider} authentication provider with the user details
   *     service and the password encoder.
   */
  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    var authenticationProvider = new DaoAuthenticationProvider();
    authenticationProvider.setUserDetailsService(userDetailsService);
    authenticationProvider.setPasswordEncoder(hashingService);
    return authenticationProvider;
  }

  /**
   * This method creates the password encoder.
   *
   * @return The {@link PasswordEncoder} instance with the hashing service.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return hashingService;
  }

  /**
   * This method creates the security filter chain. It also configures the http security.
   *
   * @param http The {@link HttpSecurity} object to configure with the security filter chain.
   * @return The {@link SecurityFilterChain} instance with the application http security
   *     configuration.
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(
        configurer ->
            configurer.configurationSource(
                request -> {
                  var cors = new CorsConfiguration();
                  cors.setAllowedOrigins(
                      java.util.Arrays.stream(allowedOrigins.split(","))
                          .map(String::trim)
                          .toList());
                  cors.setAllowedMethods(
                      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                  cors.setAllowedHeaders(List.of("*"));
                  return cors;
                }));
    http.csrf(csrfConfigurer -> csrfConfigurer.disable())
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(unauthorizedRequestHandler))
        .sessionManagement(
            customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorizeRequests ->
                authorizeRequests
                    .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/api/v1/iot/edge/**")
                    .hasRole("EDGE_SERVICE")
                    .requestMatchers(
                        "/api/v1/authentication/**",
                        "/actuator/health",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/")
                    .permitAll()
                    .anyRequest()
                    .authenticated());
    http.authenticationProvider(authenticationProvider());
    http.addFilterBefore(edgeApiKeyFilter, LogoutFilter.class);
    http.addFilterBefore(authorizationRequestFilter(), UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * This is the constructor of the class.
   *
   * @param userDetailsService The user details service.
   * @param tokenService The token service.
   * @param hashingService The hashing service.
   * @param authenticationEntryPoint The authentication entry point.
   */
  public WebSecurityConfiguration(
      @Qualifier("defaultUserDetailsService") UserDetailsService userDetailsService,
      BearerTokenService tokenService,
      BCryptHashingService hashingService,
      AuthenticationEntryPoint authenticationEntryPoint,
      EdgeApiKeyFilter edgeApiKeyFilter) {
    this.userDetailsService = userDetailsService;
    this.tokenService = tokenService;
    this.hashingService = hashingService;
    this.unauthorizedRequestHandler = authenticationEntryPoint;
    this.edgeApiKeyFilter = edgeApiKeyFilter;
  }
}
