package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "app_user")
@EntityListeners(AuditingEntityListener.class)
@Data
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int userId;
  @Column(unique = true)
  String username;
  @Column(unique = true)
  String email;
  String password;
  boolean banned = false;
  @Enumerated(EnumType.STRING)
  Role role = Role.ROLE_USER;
  @Enumerated(EnumType.STRING)
  MembershipLevel membershipLevel = MembershipLevel.FREE;
  int donation;
  @CreatedDate
  LocalDateTime lastLogin;
  @CreatedDate
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "user")
  List<Payment> payments;

  public User(String email, String username, String password, Role role) {
    this.email = email;
    this.username = username;
    this.password = password;
    this.role = role;
    this.donation = 0;
  }

  public User() {

  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return userId == user.userId;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(userId);
  }

  @Override
  public String toString() {
    return "User{" +
        "userId=" + userId +
        ", username='" + username + '\'' +
        ", email='" + email + '\'' +
        ", password='" + password + '\'' +
        ", banned=" + banned +
        ", role=" + role +
        ", membershipLevel=" + membershipLevel +
        ", donation=" + donation +
        ", lastLogin=" + lastLogin +
        ", createdAt=" + createdAt +
        ", payments=" + payments +
        '}';
  }
}
