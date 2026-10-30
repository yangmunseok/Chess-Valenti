package org.spring.createa.chessvalenti.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.service.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final UserService userService;
  private final UserDetailsService userDetailsService;

  @GetMapping("/login")
  public String loginPage() {
    return "login";
  }

  @GetMapping("/forgot-password")
  public String forgotPasswordPage() {
    return "forgot-password";
  }

  @PostMapping("/forgot-password")
  public String forgotPassword(@RequestParam("email") String email, HttpServletRequest request) {
    log.info("Password reset requested for email: {}", email);
    String contextPath =
        request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    userService.sendPasswordResetEmail(email, contextPath);
    return "redirect:/login?resetRequested=true";
  }

  @GetMapping("/reset-password")
  public String resetPasswordPage(@RequestParam("token") String token, Model model) {
    String result = userService.validatePasswordResetToken(token);
    if (result != null) {
      return "redirect:/login?error=" + result;
    }
    model.addAttribute("token", token);
    return "reset-password";
  }

  @PostMapping("/reset-password")
  public String resetPassword(@RequestParam("token") String token,
      @RequestParam("password") String password) {
    String result = userService.validatePasswordResetToken(token);
    if (result != null) {
      return "redirect:/login?error=" + result;
    }
    User user = userService.getUserByPasswordResetToken(token);
    userService.changeUserPassword(user, password);
    return "redirect:/login?resetSuccess=true";
  }

  @GetMapping("/signup")
  public String signupPage() {
    return "signup";
  }

  @PostMapping("/signup")
  public String signup(@ModelAttribute User user, HttpServletRequest request)
      throws ServletException {
    log.info("User signup request: {}", user.getUsername());
    // register 가 user 의 비밀번호를 암호화된 값으로 바꾸므로, 로그인에는 미리 꺼내 둔 평문을 쓴다
    String password = user.getPassword();
    userService.register(user);
    request.login(user.getUsername(), password);
    /*
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
    Authentication auth = new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
    );

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    HttpSession session = request.getSession(true);
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        context
    );
    */
    log.info("User {} signed up and logged in automatically", user.getUsername());
    return "redirect:/";
  }
}
