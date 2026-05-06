package org.spring.createa.chessvalenti.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.spring.createa.chessvalenti.dto.InsightPayload;
import org.spring.createa.chessvalenti.dto.InsightRequestMessage;
import org.spring.createa.chessvalenti.service.JobService;
import org.spring.createa.chessvalenti.service.LichessApi;
import org.spring.createa.chessvalenti.service.LichessService;
import org.spring.createa.chessvalenti.service.LichessService.GameResults;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class ChessInsightController {

  SimpMessagingTemplate simpMessagingTemplate;
  LichessService lichessService;
  LichessApi lichessApi;
  JobService jobService;

  public ChessInsightController(SimpMessagingTemplate simpMessagingTemplate,
      LichessService lichessService, LichessApi lichessApi, JobService jobService) {
    this.simpMessagingTemplate = simpMessagingTemplate;
    this.lichessService = lichessService;
    this.lichessApi = lichessApi;
    this.jobService = jobService;
  }

  @MessageMapping("/insight")
  public void CreateInsight(InsightRequestMessage insightRequestMessage) {
    System.out.println(insightRequestMessage);
    if (insightRequestMessage.cancel() != null && insightRequestMessage.cancel()) {
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
            try {
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
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
      sink.onCancel(() -> {
        canceled.getAndSet(false);
        InsightPayload insightPayload = new InsightPayload(insightRequestMessage.username(),
            cnt.get(), "done", "load", id, result);
        simpMessagingTemplate.convertAndSend("/topic/insight", insightPayload);

        InsightPayload insightPayload2 = new InsightPayload(insightRequestMessage.username(),
            cnt.get(), "pending", "filter", id, null);
        simpMessagingTemplate.convertAndSend("/topic/insight", insightPayload2);

        try {
          lichessService.filterSimilarGame(result);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
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
