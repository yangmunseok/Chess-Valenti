package org.spring.createa.chessvalenti.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.spring.createa.chessvalenti.dto.response.ChessComArchivesResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class ChessComServiceTest {

  @Mock
  private ChessComApi chessComApi;

  private ChessComService chessComService;

  @BeforeEach
  void setUp() {
    chessComService = new ChessComService(chessComApi);
  }

  @Test
  void loadGames_shouldFilterGamesCorrectly() {
    // Given
    String username = "testuser";
    String perfType = "blitz,rapid";
    // 2026-06-22 00:00:00 UTC = 1782086400000 ms
    String since = "1782086400000";

    // Archive URLs
    List<String> archives = List.of(
        "https://api.chess.com/pub/player/testuser/games/2026/05",
        "https://api.chess.com/pub/player/testuser/games/2026/06"
    );
    ChessComArchivesResponse archivesResponse = new ChessComArchivesResponse(archives);

    // Mock archives load
    when(chessComApi.loadArchives(username)).thenReturn(Mono.just(archivesResponse));

    // Mock monthly PGN for 2026/05 (Should be skipped if before sinceMonth, but sinceMonth (2026/06) filter applies. Wait, 1782086400000 is June 2026)
    // 2026/06 monthly PGN containing standard, blitz, rapid games
    String pgn = "[Event \"?\"]\n"
        + "[White \"testuser\"]\n"
        + "[Black \"opponent\"]\n"
        + "[Result \"1-0\"]\n"
        + "[UTCDate \"2026.06.22\"]\n"
        + "[TimeClass \"blitz\"]\n"
        + "[Variant \"Chess\"]\n"
        + "\n"
        + "1. e4 e5 1-0\n\n"
        + "[Event \"?\"]\n"
        + "[White \"opponent\"]\n"
        + "[Black \"testuser\"]\n"
        + "[Result \"0-1\"]\n"
        + "[UTCDate \"2026.06.22\"]\n"
        + "[TimeClass \"bullet\"]\n" // Bullet game, should be filtered out
        + "[Variant \"Chess\"]\n"
        + "\n"
        + "1. d4 d5 0-1\n\n"
        + "[Event \"?\"]\n"
        + "[White \"opponent\"]\n"
        + "[Black \"testuser\"]\n"
        + "[Result \"1/2-1/2\"]\n"
        + "[UTCDate \"2026.06.22\"]\n"
        + "[TimeClass \"rapid\"]\n"
        + "[Variant \"Chess\"]\n"
        + "\n"
        + "1. Nf3 Nf6 1/2-1/2";

    when(chessComApi.loadMonthlyPgn("testuser", "2026", "06")).thenReturn(Mono.just(pgn));

    // When
    var resultFlux = chessComService.loadGames(username, perfType, since);

    // Then
    StepVerifier.create(resultFlux)
        .expectNextMatches(game -> "white".equals(game.winner())
            && "testuser".equals(game.whiteUsername())
            && "opponent".equals(game.blackUsername())
            && "chess".equals(game.variant()))
        .expectNextMatches(game -> "".equals(game.winner()) // draw
            && "opponent".equals(game.whiteUsername())
            && "testuser".equals(game.blackUsername())
            && "chess".equals(game.variant()))
        .verifyComplete();
  }
}
