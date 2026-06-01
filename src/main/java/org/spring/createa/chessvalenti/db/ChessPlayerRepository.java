package org.spring.createa.chessvalenti.db;

import java.util.Optional;
import org.spring.createa.chessvalenti.domain.ChessPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChessPlayerRepository extends JpaRepository<ChessPlayer, Long>,
    ChessPlayerRepositoryCustom {

  Optional<ChessPlayer> findFirstByName(String name);
}
