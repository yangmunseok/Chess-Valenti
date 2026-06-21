package org.spring.createa.chessvalenti.util;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.game.Player;
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

  ChessPlayerRepository chessPlayerRepository;
  GameIndexRepository gameIndexRepository;

  @Getter
  int moveIndex = 0;

  int pawnMoveCnt = 0;
  @Getter
  ValentiBoard board = new ValentiBoard();
  Set<Long> visited = new HashSet<>(128);
  ChessBoardUtil chessBoardUtil;
  CustomGame game;
  ChessPlayer whitePlayer;
  ChessPlayer blackPlayer;
  private static final Map<Long, Integer> pawnStructureCnt = new HashMap<>();
  private final int maxGame = 20;
  private static final int BATCH_SIZE = 10000;
  private static final List<GameIndex> gameIndexList = new ArrayList<>(BATCH_SIZE);
  private static PrintWriter playerWriter;
  private static PrintWriter gameIndexWriter;
  private static final Map<String, ChessPlayer> playerCache = new HashMap<>();
  private static final Set<String> writtenPlayer = new HashSet<>();

  public GameProcessor(ChessBoardUtil chessBoardUtil, CustomGame game, ChessPlayer whitePlayer,
      ChessPlayer blackPlayer) {
    this.chessBoardUtil = chessBoardUtil;
    this.game = game;
    this.whitePlayer = whitePlayer;
    this.blackPlayer = blackPlayer;
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
    GameIndex gi = generateGameIndex();

    // Column order: id, pawn_structure, piece_configuration, game_offset, move_index,
    // white_player_id, black_player_id, white_elo, black_elo, max_elo, total_elo
    gameIndexWriter.print(
        gi.getPawnStructure() + "," +
            gi.getPieceConfiguration() + "," +
            gi.getGameOffset() + "," +
            gi.getMoveIndex() + "," +
            gi.getWhitePlayer().getId() + "," +
            gi.getBlackPlayer().getId() + "," +
            gi.getWhiteElo() + "," +
            gi.getBlackElo() + "," +
            gi.getMaxElo() + "," +
            gi.getTotalElo() + "\n");
  }

  private ChessPlayer getOrCreatePlayer(Player player) {
    return playerCache.computeIfAbsent(player.getName(),
        (name) -> {
          ChessPlayer chessPlayer = new ChessPlayer(name, player.getElo());
          chessPlayer.setId(playerCache.size());
          return chessPlayer;
        });
  }

  public void writeChessPlayerToCsv() {

    if (!writtenPlayer.contains(whitePlayer.getName())) {
      playerWriter.println(
          formatPlayer(whitePlayer));
    }

    if (!writtenPlayer.contains(blackPlayer.getName())) {
      playerWriter.println(
          formatPlayer(blackPlayer));
    }

    writtenPlayer.add(whitePlayer.getName());
    writtenPlayer.add(blackPlayer.getName());

  }

  private String formatPlayer(ChessPlayer chessPlayer) {
    return chessPlayer.getId() + ",\"" + chessPlayer.getName().replace("\"", "\"\"")
        + ",\"" + chessPlayer.getRating();
  }

  public boolean doMove(Move move) {
    if (pawnStructureIsMeaningful()) {
      visited.add(board.getPawnStructure());
      pawnStructureCnt.merge(board.getPawnStructure(), 1,
          (oldValue, newValue) -> Math.min(maxGame, oldValue + newValue));
    }
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
    return visited.contains(board.getPawnStructure());
  }

  public boolean hasEnoughExampleGameWithPawnStructure() {
    return pawnStructureCnt.getOrDefault(board.getPawnStructure(), 0) >= maxGame;
  }

  public GameIndex generateGameIndex() {
    return new GameIndex(game, board, whitePlayer, blackPlayer);
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