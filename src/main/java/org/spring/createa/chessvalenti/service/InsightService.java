package org.spring.createa.chessvalenti.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class InsightService {

  private final SimpMessagingTemplate simpMessagingTemplate;
  private final LichessService lichessService;
  private final LichessApi lichessApi;
  private final ChessComService chessComService;
  private final JobService jobService;
  private final InsightRepository insightRepository;

  public void createInsight(InsightRequestMessage request, User user) {
    if (Boolean.TRUE.equals(request.cancel())) {
      if (request.id() != null) {
        jobService.dispose(request.id());
      }
      return;
    }

    String systemUsername = (user != null) ? user.getUsername() : "Guest";
    Map<String, GameResults> result = new java.util.concurrent.ConcurrentHashMap<>();
    AtomicInteger cnt = new AtomicInteger();
    long id = jobService.getAvailableId();

    Mono<Object> mono = Mono.create(sink -> {
      Disposable innerSubscription = loadInsightGames(request)
          .subscribe(game -> {
            lichessService.loadGame(game, request.username(), result);
            int count = cnt.incrementAndGet();
            sendProgress(systemUsername, request.username(), count, "pending", "load", id, null);
          }, error -> {
            log.error("Error loading games from {}", platform(request), error);
            String errorMessage = "분석 중 오류가 발생했습니다.";
            if (error instanceof UserNotFoundException) {
              errorMessage = "존재하지 않는 사용자 아이디입니다.";
            }
            sendProgress(systemUsername, request.username(), 0, "error", errorMessage, id, null);
            sink.error(error);
          }, () -> {
            int count = cnt.get();
            log.info("Finished loading {} games for user {}", count, request.username());
            sendProgress(systemUsername, request.username(), count, "done", "load", id, result);
            sendProgress(systemUsername, request.username(), count, "pending", "filter", id, null);

            lichessService.filterSimilarGame(result);
            sendProgress(systemUsername, request.username(), count, "done", "filter", id, result);
            saveInsight(user, request, result);
            sink.success(result);
          });

      sink.onCancel(() -> {
        log.info("Analysis job {} cancelled for user {}. Stopping {} fetch.", id,
            request.username(), platform(request));
        innerSubscription.dispose();

        int count = cnt.get();
        sendProgress(systemUsername, request.username(), count, "done", "load", id, result);
        sendProgress(systemUsername, request.username(), count, "pending", "filter", id, null);

        lichessService.filterSimilarGame(result);
        sendProgress(systemUsername, request.username(), count, "done", "filter", id, result);
        saveInsight(user, request, result);
      });
    });

    jobService.work(mono, id);
    sendProgress(systemUsername, request.username(), 0, "done", "register", id, null);
  }

  private Flux<InsightGame> loadInsightGames(InsightRequestMessage request) {
    Flux<InsightGame> gameFlux;
    if ("chesscom".equals(platform(request))) {
      gameFlux = chessComService.loadGames(request.username(), request.perfType(), request.since());
    } else {
      gameFlux = lichessApi.loadGames(request.username(), true, request.perfType(), request.since())
          .map(response -> {
            String whiteUsername = null;
            String blackUsername = null;
            if (response.players() != null) {
              if (response.players().white() != null && response.players().white().user() != null) {
                whiteUsername = response.players().white().user().name();
              }
              if (response.players().black() != null && response.players().black().user() != null) {
                blackUsername = response.players().black().user().name();
              }
            }
            return new InsightGame(response.winner(), response.pgn(), whiteUsername,
                blackUsername, response.variant());
          });
    }

    return gameFlux.onErrorMap(WebClientResponseException.NotFound.class,
        e -> new UserNotFoundException("존재하지 않는 사용자입니다: " + request.username()));
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
