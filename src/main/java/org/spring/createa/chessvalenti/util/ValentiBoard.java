package org.spring.createa.chessvalenti.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.game.GameContext;
import com.github.bhlangonijr.chesslib.move.Move;
import lombok.Getter;

public class ValentiBoard extends Board {

  @Getter
  long pawnStructure = ChessHashHelper.hashPawnStructure(this);
  @Getter
  int pieceConfiguration = ChessHashHelper.hashPieceConfiguration(this);
  @Getter
  int plyCount = 0;

  public ValentiBoard() {
    super();
  }

  public ValentiBoard(GameContext gameContext,
      boolean updateHistory) {
    super(gameContext, updateHistory);
  }

  @Override
  public boolean doMove(Move move) {
    if (super.doMove(move)) {
      pawnStructure = ChessHashHelper.hashPawnStructure(this);
      pieceConfiguration = ChessHashHelper.hashPieceConfiguration(this);
      plyCount++;
      return true;
    }

    return false;
  }
}
