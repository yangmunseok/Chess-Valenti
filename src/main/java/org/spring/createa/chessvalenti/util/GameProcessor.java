package org.spring.createa.chessvalenti.util;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.move.Move;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spring.createa.chessvalenti.db.ChessPlayerRepository;
import org.spring.createa.chessvalenti.db.GameIndexRepository;
import org.spring.createa.chessvalenti.domain.ChessPlayer;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.spring.createa.chessvalenti.dto.game.CustomGame;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class GameProcessor {

  private final ChessPlayerRepository chessPlayerRepository;
  private final GameIndexRepository gameIndexRepository;
  private final ChessBoardUtil chessBoardUtil;

  @Getter
  int moveIndex = 0;

  int pawnMoveCnt = 0;
  @Getter
  ValentiBoard board = new ValentiBoard();
  Set<Long> visited = new HashSet<>(128);
  CustomGame game;
  ChessPlayer whitePlayer;
  ChessPlayer blackPlayer;
  private static final Map<Long, Integer> pawnStructureCnt = new HashMap<>();
  private final int maxGame = 20;
  private static final int BATCH_SIZE = 10000;
  private static final List<GameIndex> gameIndexList = new ArrayList<>(BATCH_SIZE);
  private static PrintWriter playerWriter;
  private static PrintWriter gameIndexWriter;
  private static final Set<String> writtenPlayer = new HashSet<>();
  private static long next_game_index_id = 0;

  public static void resetProcessingState() {
    pawnStructureCnt.clear();
    gameIndexList.clear();
    writtenPlayer.clear();
    next_game_index_id = 0;
  }

  public GameProcessor initialize(CustomGame game, ChessPlayer whitePlayer,
      ChessPlayer blackPlayer) {
    this.game = game;
    this.whitePlayer = whitePlayer;
    this.blackPlayer = blackPlayer;
    this.board = new ValentiBoard();
    this.visited.clear();
    return this;
  }

  public static void setPlayerWriter(PrintWriter playerWriter) {
    GameProcessor.playerWriter = playerWriter;
  }

  public static void setGameIndexWriter(PrintWriter gameIndexWriter) {
    GameProcessor.gameIndexWriter = gameIndexWriter;
  }

  public static void removeGame(CustomGame game) {
    gameIndexList.removeIf((gameIndex -> gameIndex.getGameOffset() == game.getOffset()));
  }

  public static void close() {
    playerWriter.close();
    gameIndexWriter.close();
  }

  public void saveGameIndex() {
    GameIndex gameIndex = generateGameIndex();

    gameIndexList.add(gameIndex);

    if (gameIndexList.size() == BATCH_SIZE - 1) {
      saveAndFlushGameIndexes();
    }
  }

  public void writeGameIndexToCsv() {
    GameIndex gameIndex = generateGameIndex();

    // Column order: id, pawn_structure, piece_configuration, game_offset, move_index,
    // white_player_id, black_player_id, white_elo, black_elo, max_elo, total_elo
    gameIndexWriter.println(gameIndex);
  }

  public void writeChessPlayerToCsv() {

    if (!writtenPlayer.contains(whitePlayer.getName())) {
      playerWriter.println(whitePlayer);
    }

    if (!writtenPlayer.contains(blackPlayer.getName())) {
      playerWriter.println(blackPlayer);
    }

    writtenPlayer.add(whitePlayer.getName());
    writtenPlayer.add(blackPlayer.getName());

  }


  public boolean doMove(Move move) {
    Piece piece = board.getPiece(move.getFrom());
    if (piece == Piece.WHITE_PAWN || piece == Piece.BLACK_PAWN) {
      pawnMoveCnt++;
    }
    boolean moveSuccess = board.doMove(move);

    if (moveSuccess) {
      moveIndex++;
    }

    return moveSuccess;
  }

  public boolean pawnStructureIsMeaningful() {
    return pawnMoveCnt >= 6 && chessBoardUtil.isMaterialEven(board)
        && chessBoardUtil.countCenterPawns(board) >= 4;
  }

  public boolean boardHasNewPawnStructure() {
    return !visited.contains(board.getPawnStructure());
  }

  public void recordCurrentPawnStructure() {
    long pawnStructure = board.getPawnStructure();
    visited.add(pawnStructure);
    pawnStructureCnt.merge(pawnStructure, 1,
        (oldValue, newValue) -> Math.min(maxGame, oldValue + newValue));
  }

  public boolean hasEnoughExampleGameWithPawnStructure() {
    return pawnStructureCnt.getOrDefault(board.getPawnStructure(), 0) >= maxGame;
  }

  public GameIndex generateGameIndex() {
    GameIndex gameIndex = new GameIndex(game, board, whitePlayer, blackPlayer);
    gameIndex.setId(next_game_index_id++);
    return gameIndex;
  }

  public void saveAndFlushGameIndexes() {
    if (gameIndexList.isEmpty()) {
      return;
    }
    chessPlayerRepository.insertMissingAndFillIds(collectPlayers());
    gameIndexRepository.insertAll(gameIndexList);
    gameIndexList.clear();
  }

  private List<ChessPlayer> collectPlayers() {
    Map<String, ChessPlayer> players = new HashMap<>();
    for (GameIndex gameIndex : gameIndexList) {
      ChessPlayer whitePlayer = gameIndex.getWhitePlayer();
      ChessPlayer blackPlayer = gameIndex.getBlackPlayer();
      if (whitePlayer.getId() == 0) {
        players.putIfAbsent(whitePlayer.getName(), whitePlayer);
      }
      if (blackPlayer.getId() == 0) {
        players.putIfAbsent(blackPlayer.getName(), blackPlayer);
      }
    }
    return new ArrayList<>(players.values());
  }
}
