package org.spring.createa.chessvalenti.dto.game;

import com.github.bhlangonijr.chesslib.Side;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GameResults {
  private final PlayerColor white = new PlayerColor();
  private final PlayerColor black = new PlayerColor();

  public int getTotal() {
    return white.getWhiteWon() + white.getDrawn() + white.getBlackWon() +
           black.getBlackWon() + black.getDrawn() + black.getWhiteWon();
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

  public void addResult(Side playerColor, String winnerColor) {
    if (playerColor == Side.WHITE) {
      white.addResult(winnerColor);
    } else {
      black.addResult(winnerColor);
    }
  }
}
