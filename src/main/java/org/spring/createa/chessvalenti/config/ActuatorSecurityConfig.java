package org.spring.createa.chessvalenti.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * dev 프로필에서 Prometheus가 로그인 없이 지표를 수집할 수 있도록 actuator 엔드포인트를 연다.
 *
 * <p>SecurityConfig 의 체인보다 먼저 검사되며, actuator 요청에만 적용된다.
 * prod 에서는 이 설정이 없고 엔드포인트도 노출되지 않는다.
 */
@Configuration
@Profile("dev")
public class ActuatorSecurityConfig {

  @Bean
  @Order(1)
  public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher(EndpointRequest.to("health", "info", "metrics", "prometheus"))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }
}
