package org.spring.createa.chessvalenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.spring.createa.chessvalenti.dto.response.ChessComArchivesResponse;

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
    when(chessComApi.loadArchives(username)).thenReturn(archivesResponse);

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

    JobHandle handle = new JobHandle();
    doAnswer(invocation -> {
      Consumer<String> consumer = invocation.getArgument(4);
      for (String game : pgn.split("\n\n(?=\\[Event)")) {
        consumer.accept(game.trim());
      }
      return null;
    }).when(chessComApi).loadMonthlyPgn(eq("testuser"), eq("2026"), eq("06"), eq(handle), any());

    // When
    List<InsightGame> result = new ArrayList<>();
    chessComService.loadGames(username, perfType, since, handle, result::add);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(0)).satisfies(game -> {
      assertThat(game.winner()).isEqualTo("white");
      assertThat(game.whiteUsername()).isEqualTo("testuser");
      assertThat(game.blackUsername()).isEqualTo("opponent");
      assertThat(game.variant()).isEqualTo("chess");
    });
    assertThat(result.get(1)).satisfies(game -> {
      assertThat(game.winner()).isEmpty(); // draw
      assertThat(game.whiteUsername()).isEqualTo("opponent");
      assertThat(game.blackUsername()).isEqualTo("testuser");
      assertThat(game.variant()).isEqualTo("chess");
    });
    // 2026/05는 since 이전이므로 요청하지 않음
    verify(chessComApi, never()).loadMonthlyPgn(eq("testuser"), eq("2026"), eq("05"), any(),
        any());
  }
}
