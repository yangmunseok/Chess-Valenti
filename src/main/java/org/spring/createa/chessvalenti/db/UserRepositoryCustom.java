package org.spring.createa.chessvalenti.db;

import java.time.LocalDateTime;
import java.util.List;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
  Page<User> findUsersWithFilters(String username, String email, List<String> onlineUsernames, LocalDateTime startDate, Pageable pageable);
}
