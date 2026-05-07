package org.spring.createa.chessvalenti.service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.game.GameResults;
import org.spring.createa.chessvalenti.dto.response.LichessGameResponse;
import org.spring.createa.chessvalenti.util.ChessBoardUtil;
import org.spring.createa.chessvalenti.util.ChessHashHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LichessService {

  private final LichessApi lichessApi;
  private final ChessBoardUtil chessBoardUtil;
  private final ChessHashHelper chessHashHelper;

  public void loadGame(LichessGameResponse lichessGame, String user,
      Map<String, GameResults> result) {
    if (!"standard".equals(lichessGame.variant())) {
      log.debug("Skipping non-standard game variant: {}", lichessGame.variant());
      return;
    }

    String winner = (lichessGame.winner() != null) ? lichessGame.winner() : "";

    MoveList movelist = new MoveList();
    int startIdx = lichessGame.pgn().indexOf("\n1") + 1;
    int endIdx = switch (winner) {
      case "white" -> lichessGame.pgn().lastIndexOf("1-0");
      case "black" -> lichessGame.pgn().lastIndexOf("0-1");
      default -> lichessGame.pgn().lastIndexOf("1/2-1/2");
    };

    if (startIdx <= 0 || endIdx <= startIdx) {
      log.warn("Invalid PGN format for a game");
      return;
    }

    try {
      movelist.loadFromSan(lichessGame.pgn().substring(startIdx, endIdx));
    } catch (MoveConversionException e) {
      log.error("Failed to load moves from SAN", e);
      return;
    }

    Side playerColor = Side.BLACK;
    if (lichessGame.players().white().user() != null) {
      playerColor = (lichessGame.players().white().user().name().equalsIgnoreCase(user))
          ? Side.WHITE : Side.BLACK;
    }

    Board board = new Board();
    int pawnMoveCnt = 0;
    Set<Long> visitedPawnStructure = new HashSet<>();

    for (Move move : movelist) {
      Piece piece = board.getPiece(move.getFrom());

      if (piece == Piece.BLACK_PAWN || piece == Piece.WHITE_PAWN) {
        pawnMoveCnt++;
      }

      board.doMove(move);

      if (pawnMoveCnt < 6 || !chessBoardUtil.isMaterialEven(board)
          || chessBoardUtil.countCenterPawns(board) < 4) {
        continue;
      }

      long pawnStructure = chessHashHelper.hashPawnStructure(board);

      if (visitedPawnStructure.contains(pawnStructure)) {
        continue;
      }

      visitedPawnStructure.add(pawnStructure);
      String pawnOnlyFen = chessBoardUtil.pawnsOnlyFEN(board.getFen());
      GameResults gameResults = result.computeIfAbsent(pawnOnlyFen, k -> new GameResults());
      gameResults.addResult(playerColor, winner);
    }
  }
  
  public void filterSimilarGame(Map<String, GameResults> map) {
    List<String> keys = new ArrayList<>(map.keySet());
    List<Board> boards = new ArrayList<>(keys.size());
    Set<String> removeKey = new HashSet<>();
    List<Set<Integer>> groups = new ArrayList<>();

    for (int i = 0; i < keys.size(); i++) {
      boolean groupFound = false;

      Board board = new Board();
      board.loadFromFen(keys.get(i));
      boards.add(board);

      for (Set<Integer> group : groups) {
        for (int idx : group) {
          if (chessBoardUtil.isSimilar(boards.get(i), boards.get(idx))) {
            groupFound = true;
            break;
          }
        }
        if (groupFound) {
          group.add(i);
          break;
        }
      }

      if (!groupFound) {
        Set<Integer> newGroup = new HashSet<>();
        newGroup.add(i);
        groups.add(newGroup);
      }
    }

    for (Set<Integer> group : groups) {
      if (group.size() > 1) {
        int maxIdx = 0;
        int max = 0;
        List<Integer> totals = new ArrayList<>();
        for (int idx : group) {
          String key = keys.get(idx);
          if (max < map.get(key).getTotal()) {
            max = map.get(key).getTotal();
            maxIdx = idx;
          }
        }
        int save = maxIdx;
        removeKey.addAll(
            group.stream().filter(idx -> idx != save).map(keys::get).collect(
                Collectors.toSet()));
      }
    }

    for (String key : removeKey) {
      map.remove(key);
    }
  }
}
