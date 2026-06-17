package org.spring.createa.chessvalenti.db;

import com.github.bhlangonijr.chesslib.Board;
import java.nio.file.Path;
import java.util.List;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GameIndexRepositoryCustom {

  List<GameIndex> findAllByPawnStructure(Board board);

  List<GameIndex> findAllByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn);

  Page<GameIndex> findByPawnStructure(Board board, Pageable pageable);

  Page<GameIndex> findByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn, Pageable pageable);

  void importFromCsv(Path path);

  void finishBulkInsert();
}
