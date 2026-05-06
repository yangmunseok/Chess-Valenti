package org.spring.createa.chessvalenti.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChessHashHelper {

  public ChessHashHelper() {
    initCombinationTable();
  }

  public long hashPawnStructure(Board board) {
    long whitePawn = board.getBitboard(Piece.WHITE_PAWN);
    long blackPawn = board.getBitboard(Piece.BLACK_PAWN);
    return hashPawnStructure(whitePawn, blackPawn);
  }

  public long hashPawnStructure(long whitePawn, long blackPawn) {
    long hashedWhitePawn = hashNumbers(generateInputs(whitePawn));
    long hashedBlackPawn = hashNumbers(generateInputs(blackPawn));

    return (hashedWhitePawn << 32) | (Integer.reverse((int) hashedBlackPawn) & 0xffffffffL);
  }

  public List<Integer> generateInputs(long pawns) {
    List<Integer> activeBits = getActiveBits(pawns);
    return generateInputs(activeBits);
  }

  public List<Integer> generateInputs(List<Integer> activeBits) {
    List<Integer> input = new ArrayList<>();

    // 문서 참조.
    for (int idx : activeBits) {
      input.add(idx - 8);
    }

    for (int i = 0; i < 8 - activeBits.size(); i++) {
      input.add(48 + i);
    }

    return input;
  }

  public List<Integer> getActiveBits(long input) {
    input &= 0xffffffffffff00L;

    List<Integer> output = new ArrayList<>();

    while (input != 0) {
      long t = input & -input;
      int idx = Long.numberOfTrailingZeros(t);
      output.add(idx);
      input &= input - 1;
    }

    return output;
  }

  int MAX_N = 64;
  int MAX_K = 8;
  long[][] combinationTable = new long[MAX_N + 1][MAX_K + 1];

  private void initCombinationTable() {
    for (int k = 0; k < MAX_K; k++) {
      if (k < 2) {
        combinationTable[1][k] = 1;
      } else {
        combinationTable[1][k] = 0;
      }
    }
    for (int n = 2; n <= MAX_N; n++) {
      combinationTable[n][0] = 1;
      for (int k = 1; k <= MAX_K; k++) {
        if (n < k) {
          combinationTable[n][k] = 0;
          continue;
        }
        combinationTable[n][k] = combinationTable[n - 1][k - 1] + combinationTable[n - 1][k];
      }
    }
  }

  public long hashNumbers(List<Integer> input) {
    long output = 0;
    try {
      for (int i = 0; i < input.size(); i++) {
        output += combinationTable[input.get(i)][i + 1];
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "The input list to hashNumbers must contain only non-negative integers.");
    }
    return output;
  }

  public int hashPieceConfiguration(int wq, int wr, int wb, int wn, int bq, int br, int bb,
      int bn) {
    return (wq << 13 | wr << 11 | wb << 9 | wn << 7 | bn << 5 | bb << 3 | br << 1 | bq);
  }
}
