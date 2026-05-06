package org.spring.createa.chessvalenti.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  UserRepository userRepository;
  BCryptPasswordEncoder bCryptPasswordEncoder;
  SessionRegistry sessionRegistry;

  public UserService(UserRepository userRepository, SessionRegistry sessionRegistry) {
    this.userRepository = userRepository;
    this.sessionRegistry = sessionRegistry;
    this.bCryptPasswordEncoder = new BCryptPasswordEncoder(5);
  }

  public void register(User user) {
    user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
    user.setRole(Role.ROLE_USER);
    userRepository.save(user);
  }

  public User findUserByUsername(String username) {
    return userRepository.findUserByUsername(username);
  }

  public int removeUserByUsername(String username) {
    return userRepository.removeUserByUsername(username);
  }

  public void deleteUser(User user) {
    userRepository.delete(user);
  }

  public boolean isUserOnline(String username) {
    List<Object> principals = sessionRegistry.getAllPrincipals();
    System.out.println(principals);
    for (Object principal : principals) {
      if (principal instanceof UserDetails) {
        UserDetails user = (UserDetails) principal;
        if (user.getUsername().equals(username)) {
          return true; // 세션 존재 → 온라인
        }
      }
    }
    return false; // 온라인 세션 없음 → 오프라인
  }

  public int onlineUsersCnt() {
    return sessionRegistry.getAllPrincipals().size();
  }

  public double getMemberShipRatio() {
    return userRepository.getMemberShipRatio();
  }

  public Page<User> findAll(Pageable pageable) {
    Sort sort = Sort.by(Sort.Order.desc("donation"));
    PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        sort);
    return userRepository.findAll(pageRequest);
  }

  public Page<User> findAllByUsernameOrEmail(String username, Pageable pageable) {
    Sort sort = Sort.by(Sort.Order.desc("donation"));
    PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        sort);
    return userRepository.findAll(pageRequest);
  }

  public User findUserById(int id) {
    return userRepository.findUserByUserId(id);
  }

  public void patchUserRoleById(int id, String role, Boolean ban) {
    User user = findUserById(id);
    if (role != null && role.isEmpty()) {
      user.setRole(Role.valueOf(role));
    }
    if (ban != null) {
      user.setBanned(ban);
    }
    System.out.println(ban);
    userRepository.save(user);
  }

  public void addDonation(User user, int amount) {
    user.setDonation(user.getDonation() + amount);
    userRepository.save(user);
  }

  public int countUsersByCreationDate(int year, int month, int day) {
    LocalDate date = LocalDate.of(year, month, day);
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusDays(1).atStartOfDay();
    return userRepository.countUserByCreatedAtBetween(start, end);
  }

  public int countUsersByCreationMonth(int year, int month) {
    LocalDate date = LocalDate.of(year, month, 1);
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusMonths(1).atStartOfDay();
    return userRepository.countUserByCreatedAtBetween(start, end);
  }

  public int countUsersByCreationMonthAndDonationNot(int year, int month,
      int donation) {
    LocalDate date = LocalDate.of(year, month, 1);
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusMonths(1).atStartOfDay();
    return userRepository.countUserByCreatedAtBetweenAndDonationNot(start, end,
        donation);
  }

  public void banOrUnbanUser(int id) {
    User user = userRepository.findUserByUserId(id);
    user.setBanned(!user.isBanned());
    userRepository.save(user);
  }

  public Page<User> findAllByEmail(String email, Pageable pageable) {
    Sort sort = Sort.by(Sort.Order.desc("donation"));
    PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        sort);
    return userRepository.findAllByEmailContaining(email, pageRequest);

  }
}
