package org.spring.createa.chessvalenti.service;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LichessService {

  private final LichessApi lichessApi;

  public void loadGames(String username, String perfType, String since, CancellationToken token,
      Consumer<InsightGame> consumer) {
    log.info("Loading Lichess games for {}", username);
    lichessApi.loadGames(username, perfType, since, token, response -> {
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
      consumer.accept(new InsightGame(response.winner(), response.pgn(), whiteUsername,
          blackUsername, response.variant()));
    });
  }
}
