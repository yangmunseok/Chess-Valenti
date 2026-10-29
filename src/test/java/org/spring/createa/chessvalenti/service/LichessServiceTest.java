package org.spring.createa.chessvalenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.dto.insight.InsightGame;
import org.spring.createa.chessvalenti.dto.response.LichessGameResponse;

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
    JobHandle handle = new JobHandle();

    doAnswer(invocation -> {
      Consumer<LichessGameResponse> consumer = invocation.getArgument(4);
      consumer.accept(gameResponse);
      return null;
    }).when(lichessApi).loadGames(eq("user"), eq("blitz"), eq("123456"), eq(handle), any());

    // When
    List<InsightGame> result = new ArrayList<>();
    lichessService.loadGames("user", "blitz", "123456", handle, result::add);

    // Then
    assertThat(result).singleElement().satisfies(game -> {
      assertThat(game.winner()).isEqualTo("white");
      assertThat(game.pgn()).isEqualTo("1. e4");
      assertThat(game.whiteUsername()).isEqualTo("whiteName");
      assertThat(game.blackUsername()).isEqualTo("blackName");
      assertThat(game.variant()).isEqualTo("standard");
    });
  }
}
