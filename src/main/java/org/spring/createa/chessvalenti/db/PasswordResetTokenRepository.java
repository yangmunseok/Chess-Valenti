package org.spring.createa.chessvalenti.db;

import org.spring.createa.chessvalenti.domain.PasswordResetToken;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  PasswordResetToken findByToken(String token);

  PasswordResetToken findByUser(User user);
}
