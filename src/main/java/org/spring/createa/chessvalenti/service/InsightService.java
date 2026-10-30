package org.spring.createa.chessvalenti.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.db.InsightRepository;
import org.spring.createa.chessvalenti.domain.Insight;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.game.GameResults;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.spring.createa.chessvalenti.dto.insight.PawnKey;
import org.spring.createa.chessvalenti.dto.request.InsightRequestMessage;
import org.spring.createa.chessvalenti.dto.response.InsightProgressResponse;
import org.spring.createa.chessvalenti.exception.RateLimitedException;
import org.spring.createa.chessvalenti.exception.UserNotFoundException;
import org.spring.createa.chessvalenti.util.InsightRanking;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

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
  private static final String PROGRESS_DESTINATION = "/queue/insight";
  private static final long PROGRESS_INTERVAL_MILLIS = 500;

  private final Counter successCounter = Metrics.counter("function.count",
      Tags.of("state", "success", "class", "insightService", "function", "createInsight"));
  private final Counter failCounter = Metrics.counter("function.count",
      Tags.of("state", "fail", "class", "insightService", "function", "createInsight"));

  public void createInsight(InsightRequestMessage request, User user, String sessionId) {
    if (Boolean.TRUE.equals(request.cancel())) {
      if (request.id() != null) {
        jobService.dispose(request.id(), sessionId);
      }
      return;
    }

    String systemUsername = (user != null) ? user.getUsername() : "Guest";
    Progress progress = new Progress(sessionId, systemUsername, request.username(),
        jobService.getAvailableId());

    try {
      jobService.work(progress.jobId(), sessionId,
          handle -> analyze(request, user, progress, handle));
      sendProgress(progress, 0, "done", "register");
    } catch (RuntimeException e) {
      log.warn("Failed to register analysis job {}: {}", progress.jobId(), e.getMessage());
      sendProgress(progress, 0, "error", e.getMessage());
    }
  }

  @EventListener
  public void onDisconnect(SessionDisconnectEvent event) {
    jobService.disposeAllOf(event.getSessionId());
  }

  // insight-N 작업 스레드에서 실행된다. 게임을 한 판씩 받아 처리한 뒤 필터링하고 저장한다.
  private void analyze(InsightRequestMessage request, User user, Progress progress,
      JobHandle handle) {
    Map<PawnKey, GameResults> result = new HashMap<>();
    AtomicInteger cnt = new AtomicInteger();
    AtomicLong lastSentAt = new AtomicLong(0);

    try {
      handle.fetch(() -> loadInsightGames(request, handle, game -> {
        insightGameProcessor.loadGame(game, request.username(), result);
        int count = cnt.incrementAndGet();
        // 게임마다 보내면 메시지가 너무 많으므로 일정 간격으로만 보냄
        long now = System.currentTimeMillis();
        if (now - lastSentAt.get() >= PROGRESS_INTERVAL_MILLIS) {
          lastSentAt.set(now);
          sendProgress(progress, count, "pending", "load");
        }
      }));
    } catch (RuntimeException e) {
      // 취소로 스트림이 끊긴 경우는 에러가 아니라 지금까지의 결과로 마무리한다
      if (!handle.isCancelled()) {
        log.error("Error loading games from {}", platform(request), e);
        String errorMessage = "분석 중 오류가 발생했습니다.";
        String outcome = "error";
        if (e instanceof UserNotFoundException) {
          errorMessage = "존재하지 않는 사용자 아이디입니다.";
          outcome = "user_not_found";
        } else if (e instanceof RateLimitedException) {
          errorMessage = e.getMessage();
          outcome = "rate_limited";
        }
        sendProgress(progress, 0, "error", errorMessage);
        failCounter.increment();
        recordResult(outcome);
        return;
      }
    }

    int count = cnt.get();
    if (handle.isResultDiscarded()) {
      log.info("Analysis job {} abandoned for user {}. Discarding {} loaded games.",
          progress.jobId(), request.username(), count);
      recordResult("discarded");
      return;
    }
    if (handle.isCancelled()) {
      log.info("Analysis job {} cancelled for user {}. Saving {} loaded games.",
          progress.jobId(), request.username(), count);
    } else {
      log.info("Finished loading {} games for user {}", count, request.username());
    }

    try {
      send(progress, new InsightProgressResponse(progress.systemUsername(), progress.username(),
          count, "done", "load", progress.jobId(), null, result.size()));
      sendProgress(progress, count, "pending", "filter");

      insightGameProcessor.filterSimilarGame(result);
      // 화면에 나오는 구조만 보내고 저장한다. structureCount 에는 필터링 후 전체 개수를 담는다.
      Map<String, GameResults> top = selectTop(result);
      send(progress, new InsightProgressResponse(progress.systemUsername(), progress.username(),
          count, "done", "filter", progress.jobId(), top, result.size()));
      saveInsight(user, request, top);
      successCounter.increment();
      recordResult(handle.isCancelled() ? "cancelled" : "success");
    } catch (RuntimeException e) {
      log.error("Failed to finish analysis job {}", progress.jobId(), e);
      failCounter.increment();
      recordResult("error");
      sendProgress(progress, 0, "error", "분석 중 오류가 발생했습니다.");
    }
  }

  private Map<String, GameResults> selectTop(Map<PawnKey, GameResults> result) {
    Map<String, GameResults> top = new LinkedHashMap<>();
    InsightRanking.selectTop(result).forEach((key, value) -> top.put(key.toFen(), value));
    return top;
  }

  private void loadInsightGames(InsightRequestMessage request, CancellationToken token,
      Consumer<InsightGame> consumer) {
    if ("chesscom".equals(platform(request))) {
      chessComService.loadGames(request.username(), request.perfType(), request.since(), token,
          consumer);
    } else {
      lichessService.loadGames(request.username(), request.perfType(), request.since(), token,
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

  // 분석 하나가 어떻게 끝났는지 기록한다 (success, cancelled, discarded, user_not_found, rate_limited, error)
  private static void recordResult(String result) {
    Metrics.counter("insight.job.result", "result", result).increment();
  }

  private void sendProgress(Progress progress, int loadedGame, String status, String goal) {
    send(progress, new InsightProgressResponse(progress.systemUsername(), progress.username(),
        loadedGame, status, goal, progress.jobId(), null, null));
  }

  // 요청한 STOMP 세션에게만 보낸다. 클라이언트는 /user/queue/insight 를 구독한다.
  private void send(Progress progress, InsightProgressResponse payload) {
    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    headers.setSessionId(progress.sessionId());
    headers.setLeaveMutable(true);
    simpMessagingTemplate.convertAndSendToUser(progress.sessionId(), PROGRESS_DESTINATION,
        payload, headers.getMessageHeaders());
  }

  private record Progress(String sessionId, String systemUsername, String username, long jobId) {

  }
}
