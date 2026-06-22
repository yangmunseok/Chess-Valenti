package org.spring.createa.chessvalenti.service;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.spring.createa.chessvalenti.dto.response.LichessGameResponse;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class LichessServiceTest {

  @Mock
  private LichessApi lichessApi;

  private LichessService lichessService;

  @BeforeEach
  void setUp() {
    lichessService = new LichessService(lichessApi);
  }

  @Test
  void loadGames_shouldCallApiAndMapToInsightGame() {
    // Given
    LichessGameResponse.LichessUser whiteUser = new LichessGameResponse.LichessUser("whiteName", "whiteName");
    LichessGameResponse.LichessPlayerSide white = new LichessGameResponse.LichessPlayerSide(whiteUser);
    LichessGameResponse.LichessUser blackUser = new LichessGameResponse.LichessUser("blackName", "blackName");
    LichessGameResponse.LichessPlayerSide black = new LichessGameResponse.LichessPlayerSide(blackUser);
    LichessGameResponse.LichessPlayers players = new LichessGameResponse.LichessPlayers(white, black);
    LichessGameResponse gameResponse = new LichessGameResponse("white", "1. e4", "normal", players, "standard");

    when(lichessApi.loadGames(anyString(), anyBoolean(), anyString(), anyString()))
        .thenReturn(Flux.just(gameResponse));

    // When
    Flux<InsightGame> result = lichessService.loadGames("user", "blitz", "123456");

    // Then
    StepVerifier.create(result)
        .expectNextMatches(game -> "white".equals(game.winner())
            && "1. e4".equals(game.pgn())
            && "whiteName".equals(game.whiteUsername())
            && "blackName".equals(game.blackUsername())
            && "standard".equals(game.variant()))
        .verifyComplete();
  }
}
