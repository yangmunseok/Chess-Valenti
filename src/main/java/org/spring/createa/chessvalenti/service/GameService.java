package org.spring.createa.chessvalenti.service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.game.Game;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.db.GameIndexRepository;
import org.spring.createa.chessvalenti.db.GameRepository;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.spring.createa.chessvalenti.dto.game.GameInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameService {

  private final ObjectProvider<GameRepository> gameRepositoryProvider;
  private final GameIndexRepository gameIndexRepository;

  public Game findGameByOffset(long offset) {
    GameRepository gameRepository = gameRepositoryProvider.getObject();
    return gameRepository.findGameByGameOffset(offset);
  }

  public Game getGameWithMoves(long offset, Integer idx) {
    Game game = findGameByOffset(offset);
    if (game == null) {
      log.warn("Game not found for offset: {}", offset);
      return null;
    }

    try {
      game.loadMoveText();
      game.setCurrentMoveList(game.getHalfMoves());
      game.setBoard(new Board());
      if (idx != null && idx > 0) {
        game.gotoMove(game.getCurrentMoveList(), idx - 1);
      }
      return game;
    } catch (Exception e) {
      log.error("Error loading moves for game at offset: {}", offset, e);
      return game;
    }
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
      log.error("Error creating GameInfo from GameIndex", e);
    }
    return null;
  }

  public Flux<GameInfo> findGamesByPawnStructure(Board board) {
    List<GameIndex> gameIndices = gameIndexRepository.findAllByPawnStructure(board);
    log.debug("Found {} games for pawn structure", gameIndices.size());
    return Flux.fromStream(gameIndices.stream().map(this::from));
  }

  public Page<GameInfo> findGamesByPawnStructure(Board board, Pageable pageable) {
    Page<GameIndex> gameIndices = gameIndexRepository.findByPawnStructure(board, pageable);
    return gameIndices.map(this::from);
  }

  public Flux<GameInfo> findGamesByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn) {
    List<GameIndex> gameIndices = gameIndexRepository.findAllByPawnStructureAndPieceConfiguration(
        board, wq, wr, wb, wn, bq, br, bb, bn);
    return Flux.fromStream(gameIndices.stream().map(this::from));
  }

  public Page<GameInfo> findGamesByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn, Pageable pageable) {
    Page<GameIndex> gameIndices = gameIndexRepository.findByPawnStructureAndPieceConfiguration(
        board, wq, wr, wb, wn, bq, br, bb, bn, pageable);
    return gameIndices.map(this::from);
  }

  public Flux<GameInfo> findGamesByPawnStructure(String fen) {
    try {
      Board board = new Board();
      board.loadFromFen(fen);
      return findGamesByPawnStructure(board);
    } catch (Exception ex) {
      log.error("Error loading board from FEN: {}", fen, ex);
      return null;
    }
  }

  public Page<GameInfo> findGamesByPawnStructure(String fen, Pageable pageable) {
    try {
      Board board = new Board();
      board.loadFromFen(fen);
      return findGamesByPawnStructure(board, pageable);
    } catch (Exception ex) {
      log.error("Error loading board from FEN: {}", fen, ex);
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

  public Page<GameInfo> findGamesByPawnStructureAndPieceConfiguration(String fen, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn, Pageable pageable) {
    Board board = new Board();
    board.loadFromFen(fen);
    return findGamesByPawnStructureAndPieceConfiguration(
        board, wq, wr, wb, wn, bq, br, bb, bn, pageable);
  }
}
