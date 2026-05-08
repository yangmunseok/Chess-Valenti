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
import org.spring.createa.chessvalenti.dto.response.LichessGameResponse;
import org.spring.createa.chessvalenti.util.ChessBoardUtil;
import org.spring.createa.chessvalenti.util.ChessHashHelper;

@ExtendWith(MockitoExtension.class)
public class LichessServiceTest {

  @Mock
  private LichessApi lichessApi;
  @Mock
  private ChessBoardUtil chessBoardUtil;
  @Mock
  private ChessHashHelper chessHashHelper;

  private LichessService lichessService;

  @BeforeEach
  void setUp() {
    lichessService = new LichessService(lichessApi, chessBoardUtil, chessHashHelper);
  }

  @Test
  void loadGame_addsResultToMap() {
    // Given
    String user = "testUser";
    // PGN with 6 pawn moves: e4, d4, c4, e5, d5, c5
    String pgn = "[Event \"?\"]\n[White \"testUser\"]\n[Black \"opponent\"]\n\n1. e4 e5 2. d4 d5 3. c4 c5 4. Nf3 Nc6 1-0";
    LichessGameResponse.LichessUser whiteUser = new LichessGameResponse.LichessUser(user, user);
    LichessGameResponse.LichessPlayerSide white = new LichessGameResponse.LichessPlayerSide(whiteUser);
    LichessGameResponse.LichessPlayerSide black = new LichessGameResponse.LichessPlayerSide(null);
    LichessGameResponse.LichessPlayers players = new LichessGameResponse.LichessPlayers(white, black);
    LichessGameResponse game = new LichessGameResponse("white", pgn, null, players, "standard");

    Map<String, GameResults> result = new HashMap<>();

    // Mocking utility methods to pass through some conditions in loadGame
    when(chessBoardUtil.isMaterialEven(any())).thenReturn(true);
    when(chessBoardUtil.countCenterPawns(any())).thenReturn(4);
    when(chessHashHelper.hashPawnStructure(any())).thenReturn(12345L);
    when(chessBoardUtil.pawnsOnlyFEN(any())).thenReturn("pawnFEN");

    // When
    lichessService.loadGame(game, user, result);

    // Then
    assertTrue(result.containsKey("pawnFEN"), "Result map should contain the pawn structure FEN");
    assertEquals(1, result.get("pawnFEN").getWhite().getWhiteWon());
  }
}
