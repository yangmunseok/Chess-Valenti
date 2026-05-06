package org.spring.createa.chessvalenti.service;

import com.github.bhlangonijr.chesslib.Board;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest
public class LichessApiTest {

  @Autowired
  LichessApi lichessApi;

  @Test
  @DisplayName("Lichess APi Test")
  void lichessApiTest() {
    StepVerifier.create(lichessApi.loadGames("Blue_Valenti", true, "classical"))
        .expectNextMatches(Objects::nonNull)
        .thenConsumeWhile(Objects::nonNull)  // 각각 null 아닌지 계속 확인
        .verifyComplete();
    lichessApi.loadGames("Blue_Valenti", true, "classical")
        .doOnNext(game -> System.out.println(game))
        .blockLast(); // 마지막까지 기다림
  }

  @Test
  @DisplayName("chess LIb")
  void chessLib() {
    Board board = new Board();
    board.loadFromFen("8/2p2ppp/p2p22/1p2p3/P2PP3/2P22P/1P3PP1/8 b - a3 0 10");
    System.out.println(board);
  }

}
