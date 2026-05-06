package org.spring.createa.chessvalenti.controller;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@WebMvcTest(value = {AuthController.class})
@AutoConfigureRestTestClient
public class AuthControllerTest {

  @Autowired
  RestTestClient restTestClient;
  @MockitoBean
  UserService userService;

  @Test
  void test() {
    User user = new User("alex2827@naver.com", "alex", "1234", Role.ROLE_USER);
    /*
    restTestClient
        .post()
        .uri(urlbuilder -> urlbuilder
            .path("/signup")
            .queryParam("username", "alex")
            .queryParam("email", "alex2827@naver.com")
            .queryParam("password", "1234").build()).exchange();

     */

    restTestClient.post().uri("/signup").body(user).exchange();
    verify(userService).register(any(User.class));
  }
}
