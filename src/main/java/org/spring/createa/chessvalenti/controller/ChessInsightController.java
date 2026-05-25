package org.spring.createa.chessvalenti.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.request.InsightRequestMessage;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.InsightService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChessInsightController {

  private final InsightService insightService;

  @MessageMapping("/insight")
  public void createInsight(InsightRequestMessage insightRequestMessage,
      Authentication authentication) {
    log.info("Received insight request via WebSocket: {}", insightRequestMessage);
    User user = null;
    if (authentication != null
        && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
      user = userPrincipal.getUser();
    }
    insightService.createInsight(insightRequestMessage, user);
  }
}
