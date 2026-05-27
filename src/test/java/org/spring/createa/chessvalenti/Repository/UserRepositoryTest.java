package org.spring.createa.chessvalenti.Repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
public class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("사용자 필터링 테스트: 닉네임 검색")
  void findUsersWithFilters_Username() {
    // Given
    User user1 = new User("user1@test.com", "Alice", "pass", Role.ROLE_USER);
    User user2 = new User("user2@test.com", "Bob", "pass", Role.ROLE_USER);
    userRepository.save(user1);
    userRepository.save(user2);

    // When
    Page<User> result = userRepository.findUsersWithFilters("Ali", null, null, null, PageRequest.of(0, 10));

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getUsername()).isEqualTo("Alice");
  }

  @Test
  @DisplayName("사용자 필터링 테스트: 이메일 검색")
  void findUsersWithFilters_Email() {
    // Given
    User user1 = new User("alice@test.com", "Alice", "pass", Role.ROLE_USER);
    User user2 = new User("bob@test.com", "Bob", "pass", Role.ROLE_USER);
    userRepository.save(user1);
    userRepository.save(user2);

    // When
    Page<User> result = userRepository.findUsersWithFilters(null, "bob", null, null, PageRequest.of(0, 10));

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getEmail()).isEqualTo("bob@test.com");
  }

  @Test
  @DisplayName("사용자 필터링 테스트: 온라인 유저 검색")
  void findUsersWithFilters_Online() {
    // Given
    User user1 = new User("user1@test.com", "Alice", "pass", Role.ROLE_USER);
    User user2 = new User("user2@test.com", "Bob", "pass", Role.ROLE_USER);
    userRepository.save(user1);
    userRepository.save(user2);

    // When
    List<String> onlineUsernames = List.of("Alice");
    Page<User> result = userRepository.findUsersWithFilters(null, null, onlineUsernames, null, PageRequest.of(0, 10));

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getUsername()).isEqualTo("Alice");
  }

  @Test
  @DisplayName("사용자 필터링 테스트: 가입일 검색")
  void findUsersWithFilters_StartDate() {
    // Given
    User user1 = new User("user1@test.com", "Alice", "pass", Role.ROLE_USER);
    userRepository.save(user1);
    
    // Manually setting createdAt if possible, but it's audited. 
    // In DataJpaTest, we just check if it works with current time.
    LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

    // When
    Page<User> result = userRepository.findUsersWithFilters(null, null, null, yesterday, PageRequest.of(0, 10));

    // Then
    assertThat(result.getContent()).isNotEmpty();
  }
}
