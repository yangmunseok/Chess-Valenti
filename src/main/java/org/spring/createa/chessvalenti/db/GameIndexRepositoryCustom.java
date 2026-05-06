package org.spring.createa.chessvalenti.db;

import com.github.bhlangonijr.chesslib.Board;
import java.util.List;
import org.spring.createa.chessvalenti.domain.GameIndex;

public interface GameIndexRepositoryCustom {

  List<GameIndex> findAllByPawnStructure(Board board);

  public List<GameIndex> findAllByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn);
}
