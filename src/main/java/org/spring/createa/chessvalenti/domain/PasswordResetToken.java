package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@ToString(exclude = {"user"})
public class PasswordResetToken {

  private static final int EXPIRATION = 60 * 24;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String token;

  @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
  @JoinColumn(nullable = false, name = "user_id")
  private User user;

  private LocalDateTime expiryDate;

  public PasswordResetToken(String token, User user) {
    this.token = token;
    this.user = user;
    this.expiryDate = LocalDateTime.now().plusMinutes(EXPIRATION);
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiryDate);
  }
}
