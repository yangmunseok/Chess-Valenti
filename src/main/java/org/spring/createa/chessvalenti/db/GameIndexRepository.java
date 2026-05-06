package org.spring.createa.chessvalenti.db;

import java.util.List;
import org.spring.createa.chessvalenti.domain.GameIndex;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameIndexRepository extends JpaRepository<GameIndex, Integer>,
    GameIndexRepositoryCustom {

  List<GameIndex> findAllByPawnStructure(long hashedPawnStructure);

  List<GameIndex> findAllByPawnStructureAndPieceConfiguration(long hashedPawnStructure,
      int hashedPieceConfiguration);
  
  void removeGameIndexByGameOffset(long gameOffset);
}
