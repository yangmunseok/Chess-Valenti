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

    String pgn = lichessGame.pgn();
    if (pgn == null || pgn.isBlank()) {
      log.debug("Skipping game with empty PGN");
      return;
    }

    // PGN 태그 끝부분 찾기 (보통 \n\n 또는 \r\n\r\n 다음에 수순이 시작됨)
    int moveStart = pgn.indexOf("\n\n");
    if (moveStart == -1) {
      moveStart = pgn.indexOf("\r\n\r\n");
    }
    
    // 만약 구분자가 없으면 "1. "을 찾아봄
    if (moveStart == -1) {
      moveStart = pgn.indexOf("1. ");
    } else {
      moveStart += 2; // \n\n 다음으로 이동
    }

    if (moveStart == -1) {
      log.warn("Could not find start of moves in PGN for game");
      return;
    }

    // 결과 표시 제거 (마지막 1-0, 0-1, 1/2-1/2 등)
    String moveText = pgn.substring(moveStart).trim();
    moveText = moveText.replaceAll("(1-0|0-1|1/2-1/2|\\*)$", "").trim();

    if (moveText.isEmpty()) {
      log.warn("Empty move text extracted from PGN");
      return;
    }

    MoveList movelist = new MoveList();
    try {
      movelist.loadFromSan(moveText);
    } catch (MoveConversionException e) {
      log.error("Failed to load moves from SAN: {}", moveText, e);
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
