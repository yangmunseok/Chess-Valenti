package org.spring.createa.chessvalenti.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = {AuthController.class})
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  UserService userService;

  @MockBean
  UserDetailsService userDetailsService;

  @Test
  void signup_registersUserAndRedirectsHome() throws Exception {
    // 실제 register 처럼 비밀번호를 암호화된 값으로 바꾼다
    doAnswer(invocation -> {
      invocation.getArgument(0, User.class).setPassword("encoded-password");
      return null;
    }).when(userService).register(any(User.class));

    // MockHttpServletRequest.login 은 항상 ServletException 을 던지므로 spy 로 바꿔 끼운다.
    // 실제 앱에서는 Spring Security 가 감싼 요청이 AuthenticationManager 로 로그인한다.
    MockHttpServletRequest[] signupRequest = new MockHttpServletRequest[1];

    mockMvc.perform(post("/signup")
            .with(csrf())
            .param("username", "alex")
            .param("email", "alex2827@naver.com")
            .param("password", "1234")
            .with(request -> {
              MockHttpServletRequest spied = spy(request);
              try {
                doNothing().when(spied).login(anyString(), anyString());
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
              signupRequest[0] = spied;
              return spied;
            }))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));

    verify(userService).register(any(User.class));
    // 암호화된 값이 아니라 사용자가 입력한 비밀번호로 로그인해야 한다
    verify(signupRequest[0]).login("alex", "1234");
  }
}
