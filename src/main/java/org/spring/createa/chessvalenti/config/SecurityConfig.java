package org.spring.createa.chessvalenti.config;

import java.time.LocalDateTime;
import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.security.BannedCheckFilter;
import org.spring.createa.chessvalenti.service.UserDetailServiceImpl;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final UserDetailServiceImpl userDetailsService;

  public SecurityConfig(UserDetailServiceImpl userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public BannedCheckFilter bannedCheckFilter() {
    return new BannedCheckFilter();
  }

  @Bean
  public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

    return httpSecurity
        .csrf(csrf -> csrf.ignoringRequestMatchers("/logout"))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers("/login", "/signup", "/css/**", "/", "/js/**").permitAll()
                .requestMatchers("/analysis", "/insight", "/board", "/api/games",
                    "/api/evaluation", "/api/evaluation/stream", "/games/**",
                    "/valenti-socket/**", "/study").permitAll()
                .anyRequest()
                .authenticated())
        .formLogin(
            form -> form.loginPage("/login").loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)).sessionManagement(session -> session
            .maximumSessions(1) // 최대 동시 세션 수
            .maxSessionsPreventsLogin(true) // true: 기존 세션 만료 대신 신규 로그인 차단
            .expiredUrl("/login") // 세션 만료 시 이동할 URL
            .sessionRegistry(sessionRegistry())
        ).addFilterBefore(bannedCheckFilter(), UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  AuthenticationProvider authenticationProvider(UserRepository userRepository) {
    DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
    authenticationProvider.setUserDetailsService(userDetailsService);
    authenticationProvider.setPasswordEncoder(new BCryptPasswordEncoder(5));
    return authenticationProvider;
  }

  @Bean
  public ApplicationListener<AuthenticationSuccessEvent> loginEventListener(
      UserRepository userRepository) {
    return event -> {
      String username = event.getAuthentication().getName();
      User user = userRepository.findUserByUsername(username);

      if (user != null) {
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
      }
    };
  }

  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }


}
