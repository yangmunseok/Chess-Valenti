package org.spring.createa.chessvalenti.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.dto.game.GameResults;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.spring.createa.chessvalenti.dto.insight.PawnKey;
import org.spring.createa.chessvalenti.util.ChessBoardUtil;

@ExtendWith(MockitoExtension.class)
public class InsightGameProcessorTest {

  @Mock
  private ChessBoardUtil chessBoardUtil;

  private InsightGameProcessor insightGameProcessor;

  @BeforeEach
  void setUp() {
    insightGameProcessor = new InsightGameProcessor(chessBoardUtil);
  }

  @Test
  void loadGame_addsResultToMap() {
    // Given
    String user = "testUser";
    // PGN with 6 pawn moves: e4, d4, c4, e5, d5, c5
    String pgn = "[Event \"?\"]\n[White \"testUser\"]\n[Black \"opponent\"]\n\n1. e4 e5 2. d4 d5 3. c4 c5 4. Nf3 Nc6 1-0";
    InsightGame game = new InsightGame("white", pgn, "testUser", "opponent", "standard");

    Map<PawnKey, GameResults> result = new HashMap<>();

    // Mocking utility methods to pass through some conditions in loadGame
    when(chessBoardUtil.isMaterialEven(any())).thenReturn(true);
    when(chessBoardUtil.countCenterPawns(any())).thenReturn(4);

    // When
    insightGameProcessor.loadGame(game, user, result);

    // Then
    // 6번째 폰 이동(3...c5) 직후 구조부터 기록되고, 이후 4. Nf3 Nc6 은 폰 구조가 같아 한 번만 센다
    assertEquals(1, result.size());
    PawnKey key = result.keySet().iterator().next();
    assertEquals("8/pp3ppp/8/2ppp3/2PPP3/8/PP3PPP/8 w", key.toFen());
    assertTrue(result.containsKey(key));
    assertEquals(1, result.get(key).getWhite().getWhiteWon());
  }
}
