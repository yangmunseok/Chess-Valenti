package org.spring.createa.chessvalenti.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.request.PaymentConfirmRequest;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PaymentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PaymentService paymentService;

  @Autowired
  private ObjectMapper objectMapper;

  private UserPrincipal testUser() {
    User user = new User();
    user.setUsername("testuser");
    user.setRole(Role.ROLE_USER);
    return new UserPrincipal(user);
  }

  private Authentication testAuthentication() {
    UserPrincipal principal = testUser();
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }

  @BeforeEach
  void setUpSecurityContext() {
    SecurityContextHolder.getContext().setAuthentication(testAuthentication());
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void confirmPayment_ShouldReturnResult() throws Exception {
    PaymentConfirmRequest request = new PaymentConfirmRequest("key", "1000", "orderId");
    Map<String, Object> result = new HashMap<>();
    result.put("status", "DONE");
    
    when(paymentService.confirmPayment(any(), any())).thenReturn(result);

    mockMvc.perform(post("/payment/confirm")
            .with(authentication(testAuthentication()))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"));
  }

  @Test
  void success_ShouldReturnSuccessView() throws Exception {
    mockMvc.perform(get("/payment/success")
            .with(authentication(testAuthentication()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("success"));
  }

  @Test
  void refund_ShouldReturnNoContent() throws Exception {
    mockMvc.perform(post("/payment/refund/request")
            .with(authentication(testAuthentication()))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("1"))
        .andExpect(status().isNoContent());
  }
}
