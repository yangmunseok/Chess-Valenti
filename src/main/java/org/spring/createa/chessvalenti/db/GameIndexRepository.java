package org.spring.createa.chessvalenti.db;

import java.util.List;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameIndexRepository extends JpaRepository<GameIndex, Long>,
    GameIndexRepositoryCustom {

  List<GameIndex> findAllByPawnStructure(long hashedPawnStructure);

  List<GameIndex> findAllByPawnStructureAndPieceConfiguration(long hashedPawnStructure,
      int hashedPieceConfiguration);

  @Query(
      value = """
          select g
          from GameIndex g
          where g.pawnStructure = :pawnStructure
          order by
            g.maxElo desc,
            g.totalElo desc,
            g.id desc
          """,
      countQuery = """
          select count(g)
          from GameIndex g
          where g.pawnStructure = :pawnStructure
          """
  )
  Page<GameIndex> findAllByPawnStructure(@Param("pawnStructure") long pawnStructure,
      Pageable pageable);

  @Query(
      value = """
          select g
          from GameIndex g
          where g.pawnStructure = :pawnStructure
            and g.pieceConfiguration = :pieceConfiguration
          order by
            g.maxElo desc,
            g.totalElo desc,
            g.id desc
          """,
      countQuery = """
          select count(g)
          from GameIndex g
          where g.pawnStructure = :pawnStructure
            and g.pieceConfiguration = :pieceConfiguration
          """
  )
  Page<GameIndex> findAllByPawnStructureAndPieceConfiguration(
      @Param("pawnStructure") long pawnStructure,
      @Param("pieceConfiguration") int pieceConfiguration, Pageable pageable);

  void removeGameIndexByGameOffset(long gameOffset);
}
