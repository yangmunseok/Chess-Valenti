package org.spring.createa.chessvalenti.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UserServiceTest {

  @Test
  void register_encodesPasswordAndSavesUser() {
    UserRepository userRepository = mock(UserRepository.class);
    UserService userService = new UserService(userRepository, mock(SessionRegistry.class));
    User user = new User("alex2827@naver.com", "alex", "1234", Role.ROLE_USER);

    userService.register(user);

    assertNotEquals("1234", user.getPassword());
    assertTrue(new BCryptPasswordEncoder(5).matches("1234", user.getPassword()));
    verify(userRepository).save(user);
  }

  @Test
  void findUserByUsername_returnsRepositoryResult() {
    UserRepository userRepository = mock(UserRepository.class);
    UserService userService = new UserService(userRepository, mock(SessionRegistry.class));
    User user = new User("alex2827@naver.com", "alex", "1234", Role.ROLE_USER);
    when(userRepository.findUserByUsername("alex")).thenReturn(user);

    assertSame(user, userService.findUserByUsername("alex"));
  }
}
