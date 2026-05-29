package org.spring.createa.chessvalenti.db;

import com.github.bhlangonijr.chesslib.Board;
import java.util.List;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.spring.createa.chessvalenti.util.ChessHashHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class GameIndexRepositoryCustomImpl implements GameIndexRepositoryCustom {

  @Autowired
  @Lazy
  GameIndexRepository gameIndexRepository;

  @Autowired
  ChessHashHelper chessHashHelper;

  public List<GameIndex> findAllByPawnStructure(Board board) {
    System.out.println("hashed pawn");
    System.out.println(chessHashHelper.hashPawnStructure(board));
    return gameIndexRepository.findAllByPawnStructure(chessHashHelper.hashPawnStructure(board));
  }

  public List<GameIndex> findAllByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn) {
    long hashedPawnStructure = chessHashHelper.hashPawnStructure(board);
    int hashedPieceConfiguration = chessHashHelper.hashPieceConfiguration(wq, wr, wb, wn, bq, br,
        bb, bn);
    return gameIndexRepository.findAllByPawnStructureAndPieceConfiguration(hashedPawnStructure,
        hashedPieceConfiguration);
  }

  @Override
  public Page<GameIndex> findByPawnStructure(Board board, Pageable pageable) {
    return gameIndexRepository.findAllByPawnStructure(chessHashHelper.hashPawnStructure(board),
        pageable);
  }

  @Override
  public Page<GameIndex> findByPawnStructureAndPieceConfiguration(Board board, int wq, int wr,
      int wb, int wn, int bq, int br, int bb, int bn, Pageable pageable) {
    long hashedPawnStructure = chessHashHelper.hashPawnStructure(board);
    int hashedPieceConfiguration = chessHashHelper.hashPieceConfiguration(wq, wr, wb, wn, bq, br,
        bb, bn);
    return gameIndexRepository.findAllByPawnStructureAndPieceConfiguration(hashedPawnStructure,
        hashedPieceConfiguration, pageable);
  }
}
