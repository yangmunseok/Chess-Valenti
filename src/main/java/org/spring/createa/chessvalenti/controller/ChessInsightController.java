package org.spring.createa.chessvalenti.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.GameResults;
import org.spring.createa.chessvalenti.dto.InsightPayload;
import org.spring.createa.chessvalenti.dto.InsightRequestMessage;
import org.spring.createa.chessvalenti.service.JobService;
import org.spring.createa.chessvalenti.service.LichessApi;
import org.spring.createa.chessvalenti.service.LichessService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChessInsightController {

  private final SimpMessagingTemplate simpMessagingTemplate;
  private final LichessService lichessService;
  private final LichessApi lichessApi;
  private final JobService jobService;

  @MessageMapping("/insight")
  public void createInsight(InsightRequestMessage insightRequestMessage) {
    log.info("Creating insight for request: {}", insightRequestMessage);

    if (Boolean.TRUE.equals(insightRequestMessage.cancel())) {
      if (insightRequestMessage.id() != null) {
        jobService.dispose(insightRequestMessage.id());
      }
      return;
    }

    Map<String, GameResults> result = new HashMap<>();
    AtomicInteger cnt = new AtomicInteger();
    AtomicBoolean canceled = new AtomicBoolean(false);
    long id = jobService.getAvailableId();

    Mono<Object> mono = Mono.create(sink -> {
      lichessApi.loadGames(insightRequestMessage.username(), true, insightRequestMessage.perfType(),
              insightRequestMessage.since())
          .subscribe(lichessGameResponse -> {
            if (canceled.get()) {
              return;
            }
            lichessService.loadGame(lichessGameResponse, insightRequestMessage.username(), result);
            cnt.getAndIncrement();
            InsightPayload insightPayload = new InsightPayload(insightRequestMessage.username(),
                cnt.get(), "pending", "load", id, null);
            simpMessagingTemplate.convertAndSend("/topic/insight", insightPayload);
          }, sink::error, () -> {
            InsightPayload insightPayload = new InsightPayload(insightRequestMessage.username(),
                cnt.get(), "done", "load", id, result);
            simpMessagingTemplate.convertAndSend("/topic/insight", insightPayload);

            InsightPayload insightPayload2 = new InsightPayload(insightRequestMessage.username(),
                cnt.get(), "pending", "filter", id, null);
            simpMessagingTemplate.convertAndSend("/topic/insight", insightPayload2);

            lichessService.filterSimilarGame(result);
            InsightPayload insightPayload3 = new InsightPayload(insightRequestMessage.username(),
                cnt.get(), "done", "filter", id, result);
            simpMessagingTemplate.convertAndSend("/topic/insight", insightPayload3);
            sink.success(result);
          });

      sink.onCancel(() -> {
        canceled.set(true);
        InsightPayload insightPayload = new InsightPayload(insightRequestMessage.username(),
            cnt.get(), "done", "load", id, result);
        simpMessagingTemplate.convertAndSend("/topic/insight", insightPayload);

        InsightPayload insightPayload2 = new InsightPayload(insightRequestMessage.username(),
            cnt.get(), "pending", "filter", id, null);
        simpMessagingTemplate.convertAndSend("/topic/insight", insightPayload2);

        lichessService.filterSimilarGame(result);
        InsightPayload insightPayload3 = new InsightPayload(insightRequestMessage.username(),
            cnt.get(), "done", "filter", id, result);
        simpMessagingTemplate.convertAndSend("/topic/insight", insightPayload3);
        sink.success(result);
      });
    });

    jobService.work(mono, id);
    InsightPayload insightPayload = new InsightPayload(insightRequestMessage.username(), 0, "done",
        "register", id, null);
    simpMessagingTemplate.convertAndSend("/topic/insight", insightPayload);
  }
}

