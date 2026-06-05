package org.spring.createa.chessvalenti.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.db.PasswordResetTokenRepository;
import org.spring.createa.chessvalenti.db.UserRepository;
import org.spring.createa.chessvalenti.domain.PasswordResetToken;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.response.AdminUserStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserService {

  UserRepository userRepository;
  PasswordResetTokenRepository tokenRepository;
  BCryptPasswordEncoder bCryptPasswordEncoder;
  SessionRegistry sessionRegistry;
  MailService mailService;

  public UserService(UserRepository userRepository,
      PasswordResetTokenRepository tokenRepository,
      SessionRegistry sessionRegistry, MailService mailService) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.sessionRegistry = sessionRegistry;
    this.mailService = mailService;
    this.bCryptPasswordEncoder = new BCryptPasswordEncoder(5);
  }

  @Transactional
  public void createPasswordResetTokenForUser(User user, String token) {
    PasswordResetToken existingToken = tokenRepository.findByUser(user);
    if (existingToken != null) {
      existingToken.setToken(token);
      existingToken.setExpiryDate(LocalDateTime.now().plusMinutes(60 * 24));
      tokenRepository.save(existingToken);
    } else {
      PasswordResetToken myToken = new PasswordResetToken(token, user);
      tokenRepository.save(myToken);
    }
  }

  public void sendPasswordResetEmail(String userEmail, String contextPath) {
    User user = userRepository.findUserByEmail(userEmail);
    if (user == null) {
      log.warn("Password reset requested for non-existent email: {}", userEmail);
      return;
    }
    String token = java.util.UUID.randomUUID().toString();
    createPasswordResetTokenForUser(user, token);
    
    String url = contextPath + "/reset-password?token=" + token;
    
    String htmlContent = "<div style=\"font-family: 'Pretendard', -apple-system, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px; background-color: #0A0D14; color: #FFFFFF; border-radius: 24px; border: 1px solid rgba(255,255,255,0.05);\">" +
        "<div style=\"text-align: center; margin-bottom: 30px;\">" +
        "<div style=\"display: inline-block; width: 12px; height: 12px; background-color: #3b82f6; border-radius: 50%; margin-right: 8px;\"></div>" +
        "<span style=\"font-size: 24px; font-weight: 800; letter-spacing: -0.5px;\">valenti</span>" +
        "</div>" +
        "<h1 style=\"font-size: 20px; font-weight: 700; margin-bottom: 16px; text-align: center;\">비밀번호 재설정 요청</h1>" +
        "<p style=\"font-size: 15px; color: #94a3b8; line-height: 1.6; text-align: center; margin-bottom: 32px;\">" +
        "안녕하세요, <b>" + user.getUsername() + "</b>님.<br>" +
        "비밀번호를 재설정하려면 아래 버튼을 클릭해 주세요.<br>" +
        "본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다." +
        "</p>" +
        "<div style=\"text-align: center; margin-bottom: 40px;\">" +
        "<a href=\"" + url + "\" style=\"display: inline-block; padding: 14px 32px; background-color: #3b82f6; color: #FFFFFF; text-decoration: none; border-radius: 12px; font-weight: 700; font-size: 15px; transition: all 0.3s;\">비밀번호 재설정하기</a>" +
        "</div>" +
        "<div style=\"border-top: 1px solid rgba(255,255,255,0.05); padding-top: 24px; text-align: center;\">" +
        "<p style=\"font-size: 12px; color: #4b5563; margin: 0;\">© 2026 valenti. All rights reserved.</p>" +
        "</div>" +
        "</div>";

    mailService.sendHtmlMail(user.getEmail(), "[valenti] 비밀번호 초기화 요청", htmlContent);
  }

  public String validatePasswordResetToken(String token) {
    PasswordResetToken passToken = tokenRepository.findByToken(
        token);

    if (passToken == null) {
      return "invalidToken";
    }
    if (passToken.isExpired()) {
      return "expired";
    }
    return null;
  }

  public void changeUserPassword(User user, String password) {
    user.setPassword(bCryptPasswordEncoder.encode(password));
    userRepository.save(user);
    // Optional: Delete the token after use
    PasswordResetToken token = tokenRepository.findByUser(user);
    if (token != null) {
        tokenRepository.delete(token);
    }
  }

  public User getUserByPasswordResetToken(String token) {
    return tokenRepository.findByToken(token).getUser();
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
    if (role != null && !role.isEmpty()) {
      user.setRole(Role.valueOf(role));
    }
    if (ban != null) {
      user.setBanned(ban);
    }
    log.debug("Patched user {}: role={}, ban={}", id, role, ban);
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

  public List<String> getOnlineUsernames() {
    return sessionRegistry.getAllPrincipals().stream()
        .filter(principal -> principal instanceof UserDetails)
        .map(principal -> ((UserDetails) principal).getUsername())
        .toList();
  }

  public AdminUserStatsResponse getAdminUserStats(
      String username, String email, boolean onlineOnly, LocalDateTime startDate, Pageable pageable) {

    List<String> onlineUsernames = onlineOnly ? getOnlineUsernames() : null;

    Sort sort = Sort.by(Sort.Order.desc("donation"));
    PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        sort);

    Page<User> users = userRepository.findUsersWithFilters(username, email, onlineUsernames,
        startDate, pageRequest);

    List<User> onlineUsers = users.getContent().stream()
        .filter(user -> isUserOnline(user.getUsername()))
        .toList();

    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    LocalDate lastMonth = today.minusMonths(1);

    int newUserCnt = countUsersByCreationDate(today.getYear(), today.getMonthValue(),
        today.getDayOfMonth());
    int yesterdayNewUserCnt = countUsersByCreationDate(yesterday.getYear(),
        yesterday.getMonthValue(),
        yesterday.getDayOfMonth());
    int newSupporter = countUsersByCreationMonthAndDonationNot(
        today.getYear(),
        today.getMonthValue(),
        0);
    int lastMonthSupporter = countUsersByCreationMonthAndDonationNot(
        lastMonth.getYear(), lastMonth.getMonthValue(), 0);

    return new AdminUserStatsResponse(
        users,
        onlineUsers,
        newUserCnt,
        newUserCnt - yesterdayNewUserCnt,
        onlineUsersCnt(),
        getMemberShipRatio(),
        newSupporter,
        newSupporter - lastMonthSupporter
    );
  }
}
