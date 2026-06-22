package org.spring.createa.chessvalenti.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
@RequiredArgsConstructor
public class LichessService {

  private final LichessApi lichessApi;

  public Flux<InsightGame> loadGames(String username, String perfType, String since) {
    log.info("Loading Lichess games for {}", username);
    return lichessApi.loadGames(username, true, perfType, since)
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
}
