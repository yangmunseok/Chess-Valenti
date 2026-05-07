package org.spring.createa.chessvalenti.dto.game;

import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.game.Round;

public class CustomGame extends Game {

  long offset;

  public CustomGame(String gameId, Round round) {
    super(gameId, round);
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }
}
