package org.spring.createa.chessvalenti.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.request.InsightRequestMessage;
import org.spring.createa.chessvalenti.service.InsightService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChessInsightController {

  private final InsightService insightService;

  @MessageMapping("/insight")
  public void createInsight(InsightRequestMessage insightRequestMessage, org.springframework.security.core.Authentication authentication) {
    log.info("Received insight request via WebSocket: {}", insightRequestMessage);
    org.spring.createa.chessvalenti.security.UserPrincipal userPrincipal = (org.spring.createa.chessvalenti.security.UserPrincipal) authentication.getPrincipal();
    insightService.createInsight(insightRequestMessage, userPrincipal.getUser());
  }
}
