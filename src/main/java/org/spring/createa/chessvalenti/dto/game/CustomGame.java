package org.spring.createa.chessvalenti.dto.game;

import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.game.Round;

public class CustomGame extends Game {

  long offset;

  private static final int ENGINE_RATING = 2900;
  private static final int GM_RATING = 2500;
  private static final String DEFAULT_STARTING_FEN =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  public boolean isNotGMGame() {
    int whiteElo = getWhitePlayer().getElo();
    int blackElo = getBlackPlayer().getElo();

    return Math.max(whiteElo, blackElo) > ENGINE_RATING
        || Math.max(whiteElo, blackElo) < GM_RATING;
  }

  public boolean isNotClassicalFormat() {
    String initialFen = getFen();
    return initialFen != null && !initialFen.equals(DEFAULT_STARTING_FEN);
  }

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
