package org.spring.createa.chessvalenti.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  public void createInsight(InsightRequestMessage request) {
    if (Boolean.TRUE.equals(request.cancel())) {
      if (request.id() != null) {
        jobService.dispose(request.id());
      }
      return;
    }

    Map<String, GameResults> result = new HashMap<>();
    AtomicInteger cnt = new AtomicInteger();
    AtomicBoolean canceled = new AtomicBoolean(false);
    long id = jobService.getAvailableId();

    Mono<Object> mono = Mono.create(sink -> {
      lichessApi.loadGames(request.username(), true, request.perfType(), request.since())
          .subscribe(response -> {
            if (canceled.get()) {
              return;
            }
            lichessService.loadGame(response, request.username(), result);
            int count = cnt.getAndIncrement();
            sendProgress(request.username(), count, "pending", "load", id, null);
          }, sink::error, () -> {
            int count = cnt.get();
            sendProgress(request.username(), count, "done", "load", id, result);
            sendProgress(request.username(), count, "pending", "filter", id, null);

            lichessService.filterSimilarGame(result);
            sendProgress(request.username(), count, "done", "filter", id, result);
            sink.success(result);
          });

      sink.onCancel(() -> {
        canceled.set(true);
        int count = cnt.get();
        sendProgress(request.username(), count, "done", "load", id, result);
        sendProgress(request.username(), count, "pending", "filter", id, null);

        lichessService.filterSimilarGame(result);
        sendProgress(request.username(), count, "done", "filter", id, result);
        sink.success(result);
      });
    });

    jobService.work(mono, id);
    sendProgress(request.username(), 0, "done", "register", id, null);
  }

  private void sendProgress(String username, int loadedGame, String status, String goal, Long jobId,
      Map<String, GameResults> data) {
    InsightProgressResponse payload = new InsightProgressResponse(username, loadedGame, status,
        goal, jobId, data);
    simpMessagingTemplate.convertAndSend("/topic/insight", payload);
  }
}
