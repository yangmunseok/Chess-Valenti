package org.spring.createa.chessvalenti.db;

import org.spring.createa.chessvalenti.domain.Insight;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsightRepository extends JpaRepository<Insight, Long> {
  Optional<Insight> findByUser(User user);
}
