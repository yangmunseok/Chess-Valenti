package org.spring.createa.chessvalenti.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.db.PasswordResetTokenRepository;
import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UserServiceTest {

  @Test
  void register_encodesPasswordAndSavesUser() {
    UserRepository userRepository = mock(UserRepository.class);
    UserService userService = new UserService(userRepository, mock(
        PasswordResetTokenRepository.class), mock(
        SessionRegistry.class), mock(MailService.class));
    User user = new User("alex2827@naver.com", "alex", "1234", Role.ROLE_USER);

    userService.register(user);

    assertNotEquals("1234", user.getPassword());
    assertTrue(new BCryptPasswordEncoder(5).matches("1234", user.getPassword()));
    verify(userRepository).save(user);
  }

  @Test
  void findUserByUsername_returnsRepositoryResult() {
    UserRepository userRepository = mock(UserRepository.class);
    UserService userService = new UserService(userRepository, mock(
        PasswordResetTokenRepository.class), mock(
        SessionRegistry.class), mock(MailService.class));
    User user = new User("alex2827@naver.com", "alex", "1234", Role.ROLE_USER);
    when(userRepository.findUserByUsername("alex")).thenReturn(user);

    assertSame(user, userService.findUserByUsername("alex"));
  }

  @Test
  void patchUserRoleById_UpdatesRoleAndBan() {
    UserRepository userRepository = mock(UserRepository.class);
    UserService userService = new UserService(userRepository, mock(
        PasswordResetTokenRepository.class), mock(
        SessionRegistry.class), mock(MailService.class));
    User user = new User();
    user.setUserId(1);
    when(userRepository.findUserByUserId(1)).thenReturn(user);

    userService.patchUserRoleById(1, "ROLE_ADMIN", true);

    assertEquals(Role.ROLE_ADMIN, user.getRole());
    assertTrue(user.isBanned());
    verify(userRepository).save(user);
  }

  @Test
  void getAdminUserStats_ReturnsFullStats() {
    UserRepository userRepository = mock(UserRepository.class);
    SessionRegistry sessionRegistry = mock(SessionRegistry.class);
    UserService userService = new UserService(userRepository, mock(
        PasswordResetTokenRepository.class), sessionRegistry, mock(MailService.class));

    when(userRepository.findUsersWithFilters(any(), any(), any(), any(),
        any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(
            new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList()));
    when(sessionRegistry.getAllPrincipals()).thenReturn(java.util.Collections.emptyList());
    when(userRepository.getMemberShipRatio()).thenReturn(0.0);

    var stats = userService.getAdminUserStats(null, null, false, null, PageRequest.of(0, 10));

    assertNotNull(stats);
    verify(userRepository).findUsersWithFilters(any(), any(), any(), any(),
        any(org.springframework.data.domain.Pageable.class));
  }
}
