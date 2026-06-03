package org.spring.createa.chessvalenti.db;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.spring.createa.chessvalenti.domain.MembershipLevel;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Integer>, UserRepositoryCustom {

  User findUserByUsername(String username);

  User findUserByEmail(String email);

  User findUserByUserId(int userId);

  List<User> findUserByCreatedAtBetween(LocalDateTime start,
      LocalDateTime end);

  int countUserByCreatedAtBetween(LocalDateTime start,
      LocalDateTime end);

  @NullMarked
  Page<User> findAll(Pageable pageable);

  Page<User> findAllByUsernameContainingOrEmailContainingIgnoreCase(String username, String email,
      Pageable pageable);

  Page<User> findAllByUsernameContaining(String username, Pageable pageable);

  Page<User> findAllByEmailContaining(String email, Pageable pageable);

  Page<User> findAllByBanned(boolean banned, Pageable pageable);

  Page<User> findAllByMembershipLevel(MembershipLevel membershipLevel, Pageable pageable);

  int removeUserByUsername(String username);

  void delete(User user);

  Optional<Object> findByUsername(String username);

  @Query(
      value =
          "SELECT CASE WHEN COUNT(*) = 0 THEN 0 ELSE SUM(CASE WHEN membership_level != 'FREE' THEN 1 ELSE 0 END) * 1.0 / COUNT(*) END "
              + "FROM app_user",
      nativeQuery = true
  )
  Double getMemberShipRatio();

  int countUserByCreatedAtBetweenAndMembershipLevelNot(LocalDateTime start, LocalDateTime end,
      MembershipLevel membershipLevel);

  int countUserByCreatedAtBetweenAndDonationNot(LocalDateTime start,
      LocalDateTime end, int donation);

}
