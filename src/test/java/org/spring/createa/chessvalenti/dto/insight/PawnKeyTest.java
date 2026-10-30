package org.spring.createa.chessvalenti.dto.insight;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.MoveList;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.util.ChessBoardUtil;

class PawnKeyTest {

  private final ChessBoardUtil chessBoardUtil = new ChessBoardUtil();

  @Test
  void toFen_FromStartPosition() {
    assertThat(PawnKey.of(new Board()).toFen()).isEqualTo("8/pppppppp/8/8/8/8/PPPPPPPP/8 w");
  }

  @Test
  void toFen_ShouldMatchPawnsOnlyFen() {
    MoveList moves = new MoveList();
    moves.loadFromSan("e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 f4 e5 Nf3 b5 a4 g6 h4");
    Board board = new Board();
    moves.forEach(move -> {
      board.doMove(move);
      // /pawn-games?fen= 링크가 깨지지 않도록 기존 FEN 키와 정확히 같아야 한다
      assertThat(PawnKey.of(board).toFen()).isEqualTo(chessBoardUtil.pawnsOnlyFEN(board.getFen()));
    });
  }

  @Test
  void samePawnsWithDifferentPieces_ShouldBeEqualKeys() {
    Board knightOut = new Board();
    knightOut.loadFromFen("rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1");

    assertThat(PawnKey.of(knightOut)).isEqualTo(PawnKey.of(new Board()));
  }
}
