package org.spring.createa.chessvalenti.security;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

  @Getter
  User user;
  @Getter
  int userId;
  @Getter
  boolean banned;

  public UserPrincipal(User user) {
    this.user = user;
    this.userId = user.getUserId();
    this.banned = user.isBanned();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(user.getRole().name()));
  }

  @Override
  public @Nullable String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }
}
