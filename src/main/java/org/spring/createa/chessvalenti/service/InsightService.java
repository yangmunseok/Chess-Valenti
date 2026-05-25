package org.spring.createa.chessvalenti.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.db.InsightRepository;
import org.spring.createa.chessvalenti.domain.Insight;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.game.GameResults;
import org.spring.createa.chessvalenti.dto.request.InsightRequestMessage;
import org.spring.createa.chessvalenti.dto.response.InsightProgressResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class InsightService {

  private final SimpMessagingTemplate simpMessagingTemplate;
  private final LichessService lichessService;
  private final LichessApi lichessApi;
  private final JobService jobService;
  private final InsightRepository insightRepository;

  public void createInsight(InsightRequestMessage request, User user) {
    if (Boolean.TRUE.equals(request.cancel())) {
      if (request.id() != null) {
        jobService.dispose(request.id());
      }
      return;
    }

    String systemUsername = (user != null) ? user.getUsername() : "anonymous";
    Map<String, GameResults> result = new java.util.concurrent.ConcurrentHashMap<>();
    AtomicInteger cnt = new AtomicInteger();
    long id = jobService.getAvailableId();

    Mono<Object> mono = Mono.create(sink -> {
      reactor.core.Disposable innerSubscription = lichessApi.loadGames(request.username(), true, request.perfType(), request.since())
          .subscribe(response -> {
            lichessService.loadGame(response, request.username(), result);
            int count = cnt.incrementAndGet();
            sendProgress(systemUsername, request.username(), count, "pending", "load", id, null);
          }, error -> {
            log.error("Error loading games from Lichess", error);
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
        log.info("Analysis job {} cancelled for user {}. Stopping Lichess fetch.", id, request.username());
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

  private void sendProgress(String systemUsername, String username, int loadedGame, String status, String goal, Long jobId,
      Map<String, GameResults> data) {
    InsightProgressResponse payload = new InsightProgressResponse(systemUsername, username, loadedGame, status,
        goal, jobId, data);
    simpMessagingTemplate.convertAndSend("/topic/insight", payload);
  }
}
