package org.spring.createa.chessvalenti.Repository;

import java.util.List;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface GameIndexQueryTimingRepository extends Repository<GameIndex, Long> {

  @Query("""
      select g
      from GameIndex g
      where g.pawnStructure = :pawnStructure
        and g.pieceConfiguration = :pieceConfiguration
      order by
        g.maxElo desc,
        g.totalElo desc,
        g.id desc
      """)
  List<GameIndex> findAllByPawnStructureAndPieceConfiguration(
      @Param("pawnStructure") long pawnStructure,
      @Param("pieceConfiguration") int pieceConfiguration);

  @Query(
      value = """
          select *
          from game_index force index (pawn_idx)
          where pawn_structure = :pawnStructure
            and piece_configuration = :pieceConfiguration
          order by
            max_elo desc,
            total_elo desc,
            id desc
          """,
      nativeQuery = true)
  List<GameIndex> findAllByPawnStructureAndPieceConfigurationForcePawnIndex(
      @Param("pawnStructure") long pawnStructure,
      @Param("pieceConfiguration") int pieceConfiguration);
}
