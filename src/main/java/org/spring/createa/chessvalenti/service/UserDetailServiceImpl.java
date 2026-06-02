package org.spring.createa.chessvalenti.service;

import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

  UserRepository userRepository;

  public UserDetailServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
    User user = userRepository.findUserByEmail(identifier);
    if (user == null) {
      user = userRepository.findUserByUsername(identifier);
    }
    if (user == null) {
      throw new UsernameNotFoundException("user not found with identifier: " + identifier);
    }
    return new UserPrincipal(user);
  }


}
