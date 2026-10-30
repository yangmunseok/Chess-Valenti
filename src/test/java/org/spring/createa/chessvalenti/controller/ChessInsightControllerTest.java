package org.spring.createa.chessvalenti.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.StringJoiner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.request.InsightSaveRequest;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.InsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChessInsightController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ChessInsightControllerTest {

  private static final String RESULTS = """
      {"white":{"whiteWon":3,"blackWon":1,"drawn":0},"black":{"whiteWon":0,"blackWon":2,"drawn":1}}""";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private InsightService insightService;

  private final User user = new User();

  @BeforeEach
  void setUpSecurityContext() {
    user.setUsername("testuser");
    user.setRole(Role.ROLE_USER);
    UserPrincipal principal = new UserPrincipal(user);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  // FEN 형식 검증은 서비스가 하므로 여기서는 서로 다른 키만 있으면 된다
  private static String body(String perfType, int structures) {
    StringJoiner data = new StringJoiner(",");
    for (int i = 0; i < structures; i++) {
      data.add("\"structure-" + i + "\":" + RESULTS);
    }
    return """
        {"username":"Hikaru","perfType":"%s","since":1700000000000,"data":{%s}}"""
        .formatted(perfType, data);
  }

  @Test
  void saveInsight_ShouldSaveForLoggedInUser() throws Exception {
    mockMvc.perform(post("/api/insight")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("blitz,rapid", 2)))
        .andExpect(status().isNoContent());

    ArgumentCaptor<InsightSaveRequest> request = ArgumentCaptor.forClass(InsightSaveRequest.class);
    verify(insightService).save(eq(user), request.capture());
    assertThat(request.getValue().username()).isEqualTo("Hikaru");
    assertThat(request.getValue().perfType()).isEqualTo("blitz,rapid");
    assertThat(request.getValue().data()).hasSize(2);
    assertThat(request.getValue().data().values().iterator().next().getBlack().getBlackWon())
        .isEqualTo(2);
  }

  @Test
  void saveInsight_WithUnknownPerfType_ShouldReject() throws Exception {
    mockMvc.perform(post("/api/insight")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("blitz,hyperbullet", 1)))
        .andExpect(status().isBadRequest());

    verify(insightService, never()).save(any(), any());
  }

  @Test
  void saveInsight_WithTooManyStructures_ShouldReject() throws Exception {
    mockMvc.perform(post("/api/insight")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("blitz", InsightSaveRequest.MAX_STRUCTURES + 1)))
        .andExpect(status().isBadRequest());

    verify(insightService, never()).save(any(), any());
  }
}
