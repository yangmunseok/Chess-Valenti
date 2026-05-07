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
  public void createInsight(InsightRequestMessage insightRequestMessage) {
    log.info("Received insight request via WebSocket: {}", insightRequestMessage);
    insightService.createInsight(insightRequestMessage);
  }
}
