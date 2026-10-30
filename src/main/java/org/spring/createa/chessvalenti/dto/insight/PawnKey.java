package org.spring.createa.chessvalenti.dto.insight;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;

/**
 * 폰 구조 하나. 흰 폰과 검은 폰이 있는 칸을 각각 64비트(비트보드)로 들고 있다.
 * 분석 중에는 FEN 문자열 대신 이 값을 키로 쓰고, 클라이언트에 보낼 때만 FEN으로 바꾼다.
 */
public record PawnKey(long white, long black) {

  public static PawnKey of(Board board) {
    return new PawnKey(board.getBitboard(Piece.WHITE_PAWN), board.getBitboard(Piece.BLACK_PAWN));
  }

  /**
   * ChessBoardUtil.pawnsOnlyFEN 과 같은 형식(폰만 남긴 배치 + " w")으로 바꾼다.
   */
  public String toFen() {
    StringBuilder fen = new StringBuilder();
    for (int rank = 7; rank >= 0; rank--) {
      int empty = 0;
      for (int file = 0; file < 8; file++) {
        long square = 1L << (rank * 8 + file);
        char piece = (white & square) != 0 ? 'P' : (black & square) != 0 ? 'p' : 0;
        if (piece == 0) {
          empty++;
          continue;
        }
        if (empty > 0) {
          fen.append(empty);
          empty = 0;
        }
        fen.append(piece);
      }
      if (empty > 0) {
        fen.append(empty);
      }
      if (rank > 0) {
        fen.append('/');
      }
    }
    return fen.append(" w").toString();
  }
}
