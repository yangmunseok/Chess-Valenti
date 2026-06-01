package org.spring.createa.chessvalenti.db;

import java.util.Collection;
import org.spring.createa.chessvalenti.domain.ChessPlayer;

public interface ChessPlayerRepositoryCustom {

  void insertMissingAndFillIds(Collection<ChessPlayer> chessPlayers);
}
