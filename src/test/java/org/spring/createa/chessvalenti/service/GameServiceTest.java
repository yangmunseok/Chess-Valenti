package org.spring.createa.chessvalenti.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.bhlangonijr.chesslib.game.Game;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.db.GameIndexRepository;
import org.spring.createa.chessvalenti.db.GameRepository;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.spring.createa.chessvalenti.dto.game.GameInfo;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

  @Mock
  private ObjectProvider<GameRepository> gameRepositoryProvider;

  @Mock
  private GameRepository gameRepository;

  @Mock
  private GameIndexRepository gameIndexRepository;

  @InjectMocks
  private GameService gameService;

  @Test
  void findGameByOffset_ShouldReturnGame() {
    Game game = new Game("1", null);
    when(gameRepositoryProvider.getObject()).thenReturn(gameRepository);
    when(gameRepository.findGameByGameOffset(100L)).thenReturn(game);

    Game result = gameService.findGameByOffset(100L);

    assertEquals(game, result);
    verify(gameRepository).findGameByGameOffset(100L);
  }

  @Test
  void getGameWithMoves_WhenGameExists_ShouldLoadMoves() {
    Game game = mock(Game.class);
    when(gameRepositoryProvider.getObject()).thenReturn(gameRepository);
    when(gameRepository.findGameByGameOffset(100L)).thenReturn(game);

    Game result = gameService.getGameWithMoves(100L, 1);

    assertNotNull(result);
    try {
      verify(game).loadMoveText();
    } catch (Exception e) {
      fail("Should not throw exception");
    }
  }

  @Test
  @Disabled
  void findGamesByPawnStructure_ShouldReturnFlux() {
    GameIndex index = new GameIndex();
    index.setGameOffset(100L);
    index.setMoveIndex(10);

    when(gameIndexRepository.findAllByPawnStructure(any())).thenReturn(List.of(index));
    when(gameRepositoryProvider.getObject()).thenReturn(gameRepository);
    when(gameRepository.findGameByGameOffset(100L)).thenReturn(new Game("1", null));

    Flux<GameInfo> result = gameService.findGamesByPawnStructure(
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

    StepVerifier.create(result)
        .expectNextMatches(info -> info.getGameOffset() == 100L && info.getMoveIdx() == 10)
        .verifyComplete();
  }
}
