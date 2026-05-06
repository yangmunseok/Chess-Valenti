package org.spring.createa.chessvalenti.db;

import com.github.bhlangonijr.chesslib.game.Game;

public interface GameRepository {

  Game findGameByGameOffset(long offset);

}
