package org.spring.createa.chessvalenti.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.db.InsightRepository;
import org.spring.createa.chessvalenti.domain.Insight;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.game.GameResults;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.spring.createa.chessvalenti.dto.request.InsightRequestMessage;
import org.spring.createa.chessvalenti.dto.response.InsightProgressResponse;
import org.spring.createa.chessvalenti.exception.UserNotFoundException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InsightService {

  private final SimpMessagingTemplate simpMessagingTemplate;
  private final LichessService lichessService;
  private final ChessComService chessComService;
  private final InsightGameProcessor insightGameProcessor;
  private final JobService jobService;
  private final InsightRepository insightRepository;
  private final Counter successCounter = Metrics.counter("function.count",
      Tags.of("state", "success", "class", "insightService", "function", "createInsight"));
  private final Counter failCounter = Metrics.counter("function.count",
      Tags.of("state", "fail", "class", "insightService", "function", "createInsight"));

  public void createInsight(InsightRequestMessage request, User user) {
    if (Boolean.TRUE.equals(request.cancel())) {
      if (request.id() != null) {
        jobService.dispose(request.id());
      }
      return;
    }

    String systemUsername = (user != null) ? user.getUsername() : "Guest";
    long id = jobService.getAvailableId();

    try {
      jobService.work(id, handle -> analyze(request, user, systemUsername, id, handle));
      sendProgress(systemUsername, request.username(), 0, "done", "register", id, null);
    } catch (RuntimeException e) {
      log.warn("Failed to register analysis job {}: {}", id, e.getMessage());
      sendProgress(systemUsername, request.username(), 0, "error", e.getMessage(), id, null);
    }
  }

  // insight-N 작업 스레드에서 실행된다. 게임을 한 판씩 받아 처리한 뒤 필터링하고 저장한다.
  private void analyze(InsightRequestMessage request, User user, String systemUsername, long id,
      JobHandle handle) {
    Map<String, GameResults> result = new HashMap<>();
    AtomicInteger cnt = new AtomicInteger();

    handle.beginFetch();
    try {
      loadInsightGames(request, handle, game -> {
        insightGameProcessor.loadGame(game, request.username(), result);
        int count = cnt.incrementAndGet();
        sendProgress(systemUsername, request.username(), count, "pending", "load", id, null);
      });
    } catch (RuntimeException e) {
      // 취소로 스트림이 끊긴 경우는 에러가 아니라 지금까지의 결과로 마무리한다
      if (!handle.isCancelled()) {
        log.error("Error loading games from {}", platform(request), e);
        String errorMessage = "분석 중 오류가 발생했습니다.";
        if (e instanceof UserNotFoundException) {
          errorMessage = "존재하지 않는 사용자 아이디입니다.";
        }
        sendProgress(systemUsername, request.username(), 0, "error", errorMessage, id, null);
        failCounter.increment();
        return;
      }
    } finally {
      handle.endFetch();
    }

    int count = cnt.get();
    if (handle.isCancelled()) {
      log.info("Analysis job {} cancelled for user {}. Saving {} loaded games.", id,
          request.username(), count);
    } else {
      log.info("Finished loading {} games for user {}", count, request.username());
    }

    try {
      sendProgress(systemUsername, request.username(), count, "done", "load", id, result);
      sendProgress(systemUsername, request.username(), count, "pending", "filter", id, null);

      insightGameProcessor.filterSimilarGame(result);
      sendProgress(systemUsername, request.username(), count, "done", "filter", id, result);
      saveInsight(user, request, result);
      successCounter.increment();
    } catch (RuntimeException e) {
      log.error("Failed to finish analysis job {}", id, e);
      failCounter.increment();
      sendProgress(systemUsername, request.username(), 0, "error", "분석 중 오류가 발생했습니다.", id,
          null);
    }
  }

  private void loadInsightGames(InsightRequestMessage request, JobHandle handle,
      Consumer<InsightGame> consumer) {
    if ("chesscom".equals(platform(request))) {
      chessComService.loadGames(request.username(), request.perfType(), request.since(), handle,
          consumer);
    } else {
      lichessService.loadGames(request.username(), request.perfType(), request.since(), handle,
          consumer);
    }
  }

  private String platform(InsightRequestMessage request) {
    return request.platform() == null || request.platform().isBlank()
        ? "lichess"
        : request.platform().toLowerCase(Locale.ROOT);
  }

  private void saveInsight(User user, InsightRequestMessage request,
      Map<String, GameResults> result) {
    if (user == null) {
      return;
    }
    Insight insight = insightRepository.findByUser(user)
        .orElse(new Insight());

    insight.setUser(user);
    insight.setLichessUsername(request.username());
    insight.setPerfType(request.perfType());
    try {
      insight.setSince(Long.parseLong(request.since()));
    } catch (NumberFormatException e) {
      log.warn("Invalid since value: {}", request.since());
    }
    insight.setData(result);
    insight.setCreatedAt(LocalDateTime.now());

    insightRepository.save(insight);
  }

  private void sendProgress(String systemUsername, String username, int loadedGame, String status,
      String goal, Long jobId,
      Map<String, GameResults> data) {
    InsightProgressResponse payload = new InsightProgressResponse(systemUsername, username,
        loadedGame, status,
        goal, jobId, data);
    simpMessagingTemplate.convertAndSend("/topic/insight", payload);
  }
}
