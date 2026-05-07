package org.spring.createa.chessvalenti.controller;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
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
    when(userDetailsService.loadUserByUsername("alex2827@naver.com"))
        .thenReturn(org.springframework.security.core.userdetails.User
            .withUsername("alex2827@naver.com")
            .password("1234")
            .roles("USER")
            .build());

    mockMvc.perform(post("/signup")
            .with(csrf())
            .param("username", "alex")
            .param("email", "alex2827@naver.com")
            .param("password", "1234"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
    verify(userService).register(any(User.class));
  }
}
