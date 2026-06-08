package org.spring.createa.chessvalenti.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.insight.PawnStructureSummary;
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

  public PawnStructureSummary summarizePawnStructure(Board board) {
    long whitePawnBB = board.getBitboard(Piece.WHITE_PAWN);
    long blackPawnBB = board.getBitboard(Piece.BLACK_PAWN);
    int totalPawns = Long.bitCount(whitePawnBB) + Long.bitCount(blackPawnBB);

    whitePawnBB = excludeWingPawn(whitePawnBB);
    blackPawnBB = excludeWingPawn(blackPawnBB);

    return new PawnStructureSummary(
        whitePawnBB,
        blackPawnBB,
        countDoubled(whitePawnBB),
        countDoubled(blackPawnBB),
        countIsolated(whitePawnBB),
        countIsolated(blackPawnBB),
        totalPawns
    );
  }

  public boolean isSimilar(PawnStructureSummary s1, PawnStructureSummary s2) {
    if (s1.totalPawns() != s2.totalPawns()) {
      return false;
    }

    int board1Pawns = Long.bitCount(s1.whitePawns()) + Long.bitCount(s1.blackPawns());
    int board2Pawns = Long.bitCount(s2.whitePawns()) + Long.bitCount(s2.blackPawns());

    if (board1Pawns + board2Pawns == 0) {
      return false;
    }

    int equalPawns = Long.bitCount((s1.whitePawns() & s2.whitePawns()) |
        (s1.blackPawns() & s2.blackPawns()));

    int similarity = (200 * equalPawns) / (board1Pawns + board2Pawns);

    if (s1.whiteDoubled() != s2.whiteDoubled()) return false;
    if (s1.blackDoubled() != s2.blackDoubled()) return false;
    if (s1.whiteIsolated() != s2.whiteIsolated()) return false;
    if (s1.blackIsolated() != s2.blackIsolated()) return false;

    return similarity > 80;
  }

  public boolean isSimilar(Board board1, Board board2) {
    return isSimilar(summarizePawnStructure(board1), summarizePawnStructure(board2));
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
