package org.spring.createa.chessvalenti.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.request.InsightSaveRequest;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.InsightService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Insight REST API", description = "인사이트 분석 결과 저장 API")
@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ChessInsightController {

  private final InsightService insightService;

  @Operation(summary = "인사이트 결과 저장",
      description = "브라우저에서 계산한 폰 구조 통계를 로그인한 사용자의 인사이트로 저장합니다. 이전 결과는 덮어씁니다.")
  @PostMapping("/insight")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void saveInsight(
      @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Valid @RequestBody InsightSaveRequest body) {
    log.info("Saving insight of {} ({} structures) for user {}", body.username(),
        body.data().size(), userPrincipal.getUsername());
    insightService.save(userPrincipal.getUser(), body);
  }
}
