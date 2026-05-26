package org.spring.createa.chessvalenti.controller;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.spring.createa.chessvalenti.dto.response.StockfishEvaluationResponse;
import org.spring.createa.chessvalenti.service.StockfishService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class StockfishController {

  private final StockfishService stockfishService;

  @GetMapping("/api/evaluation")
  public ResponseEntity<StockfishEvaluationResponse> evaluate(@RequestParam String fen) {
    return ResponseEntity.ok(stockfishService.evaluate(fen));
  }

  @GetMapping("/api/evaluation/stream")
  public SseEmitter streamEvaluation(@RequestParam String fen) {
    SseEmitter emitter = new SseEmitter(0L);
    AtomicBoolean connected = new AtomicBoolean(true);

    emitter.onCompletion(() -> connected.set(false));
    emitter.onTimeout(() -> connected.set(false));
    emitter.onError(error -> connected.set(false));

    CompletableFuture.runAsync(() -> {
      try {
        stockfishService.streamEvaluation(fen, evaluation -> {
          try {
            emitter.send(evaluation);
          } catch (IOException e) {
            connected.set(false);
            throw new IllegalStateException(e);
          }
        }, connected::get);
        if (connected.get()) {
          emitter.send(SseEmitter.event().name("done").data("ok"));
        }
        emitter.complete();
      } catch (Exception e) {
        connected.set(false);
        emitter.completeWithError(e);
      }
    });

    return emitter;
  }
}
