package org.spring.createa.chessvalenti.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
public class UserServiveTest {

  @Autowired
  UserService userService;

  static User user;

  @Test
  void test1() {
    user = new User("alex2827@naver.com", "alex", "1234", Role.ROLE_USER);
    userService.register(user);
    Assertions.assertNotNull(userService.findUserByUsername("alex"));
    userService.deleteUser(user);
  }

  @Test
  void test2() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(5);
    Assertions.assertEquals(encoder.encode("12345"),
        "$2a$05$3fuWlPVDuGswjNHd9Imc9eJBSQ08eB/jKRF4giRMSvRHP27AmB/Pi");
  }
}
