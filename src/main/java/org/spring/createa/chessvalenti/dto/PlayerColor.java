package org.spring.createa.chessvalenti.dto;

import com.github.bhlangonijr.chesslib.Side;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlayerColor {
  private int whiteWon;
  private int blackWon;
  private int drawn;

  public void addResult() {
    drawn++;
  }

  public void addResult(Side winnerColor) {
    if (winnerColor == Side.WHITE) {
      whiteWon++;
    } else if (winnerColor == Side.BLACK) {
      blackWon++;
    }
  }

  public void addResult(String winnerColor) {
    if ("white".equals(winnerColor)) {
      addResult(Side.WHITE);
    } else if ("black".equals(winnerColor)) {
      addResult(Side.BLACK);
    } else if (winnerColor == null || winnerColor.isBlank()) {
      addResult();
    } else {
      throw new IllegalArgumentException("Invalid winner color: " + winnerColor);
    }
  }
}
