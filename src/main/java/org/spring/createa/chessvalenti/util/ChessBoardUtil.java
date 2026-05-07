package org.spring.createa.chessvalenti.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ChessBoardUtil {

  private static final long FILE_A = 0x0101010101010101L;
  private static final long FILE_B = FILE_A << 1;
  private static final long FILE_C = FILE_A << 2;
  private static final long FILE_D = FILE_A << 3;
  private static final long FILE_E = FILE_A << 4;
  private static final long FILE_F = FILE_A << 5;
  private static final long FILE_G = FILE_A << 6;
  private static final long FILE_H = FILE_A << 7;

  int countDoubled(long pawns) {
    int count = 0;
    for (int file = 0; file < 8; file++) {
      long fileMask = FILE_A << file;
      long filePawns = pawns & fileMask;

      int n = Long.bitCount(filePawns);
      if (n >= 2) {
        count++;
      }
    }
    return count;
  }

  int countIsolated(long pawns) {
    int count = 0;

    for (int file = 0; file < 8; file++) {
      long fileMask = FILE_A << file;
      long pawnsOnFile = pawns & fileMask;

      if (pawnsOnFile == 0) {
        continue;
      }

      long leftMask = (file > 0) ? (FILE_A << (file - 1)) : 0;
      long rightMask = (file < 7) ? (FILE_A << (file + 1)) : 0;

      if ((pawns & (leftMask | rightMask)) == 0) {
        count += Long.bitCount(pawnsOnFile);
      }
    }

    return count;
  }

  private int bitCount(long bits) {
    return Long.bitCount(bits);
  }

  private long excludeWingPawn(long pawns) {
    return pawns & (FILE_B | FILE_C | FILE_D | FILE_E | FILE_F | FILE_G);
  }

  public int countCenterPawns(Board board) {
    long boardWhitePawnBB = board.getBitboard(Piece.WHITE_PAWN);
    long boardBlackPawnBB = board.getBitboard(Piece.BLACK_PAWN);
    return bitCount((boardWhitePawnBB | boardBlackPawnBB) & (FILE_C | FILE_D | FILE_E | FILE_F));
  }

  public boolean isSimilar(Board board1, Board board2) {
    long board1WhitePawnBB = board1.getBitboard(Piece.WHITE_PAWN);
    long board1BlackPawnBB = board1.getBitboard(Piece.BLACK_PAWN);
    long board2WhitePawnBB = board2.getBitboard(Piece.WHITE_PAWN);
    long board2BlackPawnBB = board2.getBitboard(Piece.BLACK_PAWN);

    if ((bitCount(board1WhitePawnBB) + bitCount(board1BlackPawnBB)) != 
        (bitCount(board2WhitePawnBB) + bitCount(board2BlackPawnBB))) {
      return false;
    }

    board1WhitePawnBB = excludeWingPawn(board1WhitePawnBB);
    board1BlackPawnBB = excludeWingPawn(board1BlackPawnBB);
    board2WhitePawnBB = excludeWingPawn(board2WhitePawnBB);
    board2BlackPawnBB = excludeWingPawn(board2BlackPawnBB);

    int board1Pawns = bitCount(board1WhitePawnBB) + bitCount(board1BlackPawnBB);
    int board2Pawns = bitCount(board2WhitePawnBB) + bitCount(board2BlackPawnBB);

    if (board1Pawns + board2Pawns == 0) {
      log.debug("No center pawns found for similarity comparison");
      return false;
    }

    int equalPawns = bitCount((board1WhitePawnBB & board2WhitePawnBB) | 
                              (board1BlackPawnBB & board2BlackPawnBB));

    int similarity = (200 * equalPawns) / (board1Pawns + board2Pawns);

    if (countDoubled(board1WhitePawnBB) != countDoubled(board2WhitePawnBB)) return false;
    if (countDoubled(board1BlackPawnBB) != countDoubled(board2BlackPawnBB)) return false;
    if (countIsolated(board1WhitePawnBB) != countIsolated(board2WhitePawnBB)) return false;
    if (countIsolated(board1BlackPawnBB) != countIsolated(board2BlackPawnBB)) return false;

    return similarity > 80;
  }

  public int calculateMaterialScore(Board board, Side side) {
    if (side == Side.WHITE) {
      return Long.bitCount(board.getBitboard(Piece.WHITE_PAWN)) +
          Long.bitCount(board.getBitboard(Piece.WHITE_KNIGHT)) * 3 +
          Long.bitCount(board.getBitboard(Piece.WHITE_BISHOP)) * 3 +
          Long.bitCount(board.getBitboard(Piece.WHITE_ROOK)) * 5 +
          Long.bitCount(board.getBitboard(Piece.WHITE_QUEEN)) * 9;
    }
    return Long.bitCount(board.getBitboard(Piece.BLACK_PAWN)) +
        Long.bitCount(board.getBitboard(Piece.BLACK_KNIGHT)) * 3 +
        Long.bitCount(board.getBitboard(Piece.BLACK_BISHOP)) * 3 +
        Long.bitCount(board.getBitboard(Piece.BLACK_ROOK)) * 5 +
        Long.bitCount(board.getBitboard(Piece.BLACK_QUEEN)) * 9;
  }

  public boolean isMaterialEven(Board board) {
    return calculateMaterialScore(board, Side.WHITE) == calculateMaterialScore(board, Side.BLACK);
  }

  public String pawnsOnlyFEN(String fen) {
    String board = fen.split(" ")[0];
    String[] ranks = board.split("/");

    StringBuilder newBoard = new StringBuilder();

    for (int i = 0; i < ranks.length; i++) {
      String rank = ranks[i];
      StringBuilder newRank = new StringBuilder();

      for (char c : rank.toCharArray()) {
        if (Character.isDigit(c)) {
          int empty = c - '0';
          for (int j = 0; j < empty; j++) {
            newRank.append('1');
          }
        } else if (c == 'P' || c == 'p') {
          newRank.append(c);
        } else {
          newRank.append('1'); // 다른 기물 → 빈칸
        }
      }

      // 압축
      newBoard.append(compressEmptySquares(newRank.toString()));
      if (i != ranks.length - 1) {
        newBoard.append('/');
      }
    }
    newBoard.append(" w");
    return newBoard.toString();
  }

  private String compressEmptySquares(String rank) {
    StringBuilder result = new StringBuilder();
    int count = 0;

    for (char c : rank.toCharArray()) {
      if (c == '1') {
        count++;
      } else {
        if (count > 0) {
          result.append(count);
          count = 0;
        }
        result.append(c);
      }
    }

    if (count > 0) {
      result.append(count);
    }

    return result.toString();
  }
}
