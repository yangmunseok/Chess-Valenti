package org.spring.createa.chessvalenti.service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.game.Game;
import java.util.List;
import org.spring.createa.chessvalenti.db.GameIndexRepository;
import org.spring.createa.chessvalenti.db.GameRepository;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.spring.createa.chessvalenti.dto.GameInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class GameService {

  ObjectProvider<GameRepository> gameRepositoryProvider;
  GameIndexRepository gameIndexRepository;

  public GameService(ObjectProvider<GameRepository> gameRepositoryProvider,
      GameIndexRepository gameIndexRepository) {
    this.gameRepositoryProvider = gameRepositoryProvider;
    this.gameIndexRepository = gameIndexRepository;
  }

  public Game findGameByOffset(long offset) {
    GameRepository gameRepository = gameRepositoryProvider.getObject();
    return gameRepository.findGameByGameOffset(offset);
  }

  private GameInfo from(GameIndex gameIndex) {
    try {
      GameRepository gameRepository = gameRepositoryProvider.getObject();
      GameInfo gameInfo = new GameInfo();
      gameInfo.setGameOffset(gameIndex.getGameOffset());
      gameInfo.setMoveIdx(gameIndex.getMoveIndex());
      gameInfo.setGame(gameRepository.findGameByGameOffset(gameIndex.getGameOffset()));
      //gameInfo.setPgn(gameInfo.getGame().toPgn(true, true));
      return gameInfo;
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    return null;
  }

  public Flux<GameInfo> findGamesByPawnStructure(Board board) {
    List<GameIndex> gameIndices = gameIndexRepository.findAllByPawnStructure(board);
    System.out.println("repository done.");
    return Flux.fromStream(gameIndices.stream().map(this::from));
  }

  public Flux<GameInfo> findGamesByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn) {
    List<GameIndex> gameIndices = gameIndexRepository.findAllByPawnStructureAndPieceConfiguration(
        board, wq, wr, wb, wn, bq, br, bb, bn);
    return Flux.fromStream(gameIndices.stream().map(this::from));
  }

  public Flux<GameInfo> findGamesByPawnStructure(String fen) {
    try {
      Board board = new Board();
      board.loadFromFen(fen);
      return findGamesByPawnStructure(board);
    } catch (Exception ex) {
      System.out.println(ex);
      return null;
    }
  }

  public Flux<GameInfo> findGamesByPawnStructureAndPieceConfiguration(String fen, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn) {
    Board board = new Board();
    board.loadFromFen(fen);
    return findGamesByPawnStructureAndPieceConfiguration(
        board, wq, wr, wb, wn, bq, br, bb, bn);
  }
}
