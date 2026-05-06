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
import lombok.Getter;
import org.spring.createa.chessvalenti.dto.LichessGameResponse;
import org.spring.createa.chessvalenti.util.ChessBoardUtil;
import org.spring.createa.chessvalenti.util.ChessHashHelper;
import org.springframework.stereotype.Service;

@Service
public class LichessService {

  LichessApi lichessApi;
  ChessBoardUtil chessBoardUtil;
  ChessHashHelper chessHashHelper;

  public LichessService(LichessApi lichessApi, ChessBoardUtil chessBoardUtil,
      ChessHashHelper chessHashHelper) {
    this.lichessApi = lichessApi;
    this.chessBoardUtil = chessBoardUtil;
    this.chessHashHelper = chessHashHelper;
  }

  public static class GameResults {

    public static class PlayerColor {

      @Getter
      int whiteWon;
      @Getter
      int blackWon;
      @Getter
      int drawn;

      public void addResult() {
        drawn++;
      }

      public void addResult(Side winnerColor) {
        if (winnerColor == Side.WHITE) {
          whiteWon++;
        }
        blackWon++;
      }

      public void addResult(String winnerColor) throws Exception {
        if (winnerColor.equals("white")) {
          addResult(Side.WHITE);
        } else if (winnerColor.equals("black")) {
          addResult(Side.BLACK);
        } else if (winnerColor.isBlank()) {
          addResult();
        } else {
          throw new Exception("Invalid Arg on AddResult");
        }
      }
    }

    @Getter
    PlayerColor white = new PlayerColor();
    @Getter
    PlayerColor black = new PlayerColor();

    public int getTotal() {
      return white.whiteWon + white.drawn + white.blackWon + black.blackWon + black.drawn
          + black.whiteWon;
    }

    public void addResult(Side playerColor) {
      if (playerColor == Side.WHITE) {
        white.addResult();
      } else {
        black.addResult();
      }
    }

    public void addResult(Side playerColor, Side winnerColor) {
      if (playerColor == Side.WHITE) {
        white.addResult(winnerColor);
      } else {
        black.addResult(winnerColor);
      }
    }

    public void addResult(Side playerColor, String winnerColor) throws Exception {
      if (playerColor == Side.WHITE) {
        white.addResult(winnerColor);
      } else {
        black.addResult(winnerColor);
      }
    }
  }

  /*
  public Mono<Map<String, GameResults>> getUserInsight(String user, String perfType) {
    Map<String, GameResults> result = new HashMap<>();
    return Mono.create(sink -> {
      lichessApi.loadGames(user, true, perfType).subscribe(lichessGame -> {
        loadGame(lichessGame,user,result);

        });

      }, sink::error, () -> {
        try {
          sink.success(filterSimilarGame(result));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
      sink.onCancel(() -> {
        try {
          sink.success(filterSimilarGame(result));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    });
  }
*/
  public void loadGame(LichessGameResponse lichessGame,
      String user, Map<String, GameResults> result) {
    if (!lichessGame.variant().equals("standard")) {
      return;
    }
    String winner = (lichessGame.winner() != null) ? lichessGame.winner()
        : ""; //status is white,black or ""

    MoveList movelist = new MoveList();
    int startIdx = lichessGame.pgn().indexOf("\n1") + 1;
    int endIdx = 0;
    if (winner.equals("white")) {
      endIdx = lichessGame.pgn().lastIndexOf("1-0");
    }
    if (winner.equals("black")) {
      endIdx = lichessGame.pgn().lastIndexOf("0-1");
    }
    if (winner.isBlank()) {
      endIdx = lichessGame.pgn().lastIndexOf("1/2-1/2");
    }

    try {
      movelist.loadFromSan(lichessGame.pgn().substring(startIdx, endIdx));
    } catch (MoveConversionException e) {
      return;
    }

    Side playerColor = Side.BLACK;
    if (lichessGame.players().white().user() != null) {
      playerColor = (lichessGame.players().white().user().name()
          .equalsIgnoreCase(user)) ? Side.WHITE : Side.BLACK;
    }

    //play through game to getPawnStructures
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
      GameResults gameResults =
          (result.containsKey(pawnOnlyFen)) ? result.get(pawnOnlyFen) : new GameResults();
      try {
        gameResults.addResult(playerColor, winner);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      result.put(pawnOnlyFen, gameResults);
    }
  }

  /*
    public Map<String, GameResults> getInsightFromUserGames(String user, String perfType)
        throws Exception {
      Map<Long, String> pawnStructureFenDictionary = new HashMap<>();
      Map<String, GameResults> result = new HashMap<>();

      lichessApi.loadGames(user, true, perfType).doOnNext(lichessGame -> {
        if (!lichessGame.variant().equals("standard")) {
          return;
        }
        String winner = (lichessGame.winner() != null) ? lichessGame.winner()
            : ""; //status is white,black or ""

        MoveList movelist = new MoveList();
        int startIdx = lichessGame.pgn().indexOf("\n1") + 1;
        int endIdx = 0;
        if (winner.equals("white")) {
          endIdx = lichessGame.pgn().lastIndexOf("1-0");
        }
        if (winner.equals("black")) {
          endIdx = lichessGame.pgn().lastIndexOf("0-1");
        }
        if (winner.isBlank()) {
          endIdx = lichessGame.pgn().lastIndexOf("1/2-1/2");
        }

        try {
          movelist.loadFromSan(lichessGame.pgn().substring(startIdx, endIdx));
        } catch (MoveConversionException e) {
          return;
        }

        Side playerColor = Side.BLACK;
        if (lichessGame.players().white().user() != null) {
          playerColor = (lichessGame.players().white().user().name()
              .equalsIgnoreCase(user)) ? Side.WHITE : Side.BLACK;
        }

        //play through game to getPawnStructures
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
          pawnStructureFenDictionary.put(pawnStructure, pawnOnlyFen);
          GameResults gameResults =
              (result.containsKey(pawnOnlyFen)) ? result.get(pawnOnlyFen) : new GameResults();
          try {
            gameResults.addResult(playerColor, winner);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          result.put(pawnOnlyFen, gameResults);
        }
      }).blockLast();
      List<String> keys = new ArrayList<>(result.keySet());
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
            if (max < result.get(key).getTotal()) {
              max = result.get(key).getTotal();
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
        result.remove(key);
      }

      return result;
    }
  */
  public void filterSimilarGame(Map<String, GameResults> map)
      throws Exception {
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
