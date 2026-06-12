package org.spring.createa.chessvalenti.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.spring.createa.chessvalenti.domain.Payment;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.response.AdminUserStatsResponse;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.InquiryService;
import org.spring.createa.chessvalenti.service.PaymentService;
import org.spring.createa.chessvalenti.service.PostService;
import org.spring.createa.chessvalenti.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminViewController {

  private final InquiryService inquiryService;
  private final PaymentService paymentService;
  private final UserService userService;
  private final PostService postService;

  @GetMapping("/users")
  public String userListPage(@PageableDefault Pageable pageable,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String username,
      @RequestParam(required = false, defaultValue = "false") boolean onlineOnly,
      @RequestParam(required = false) LocalDate startDate,
      Model model) {

    LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
    AdminUserStatsResponse stats = userService.getAdminUserStats(username, email, onlineOnly,
        startDateTime, pageable);

    model.addAttribute("users", stats.users());
    model.addAttribute("loginUsers", stats.onlineUsers());
    model.addAttribute("totalPages", stats.users().getTotalPages());
    model.addAttribute("currentPage", pageable.getPageNumber());
    model.addAttribute("newUsersCnt", stats.newUsersCnt());
    model.addAttribute("diffNewUser", stats.diffNewUser());
    model.addAttribute("onlineUserCnt", stats.onlineUserCnt());
    model.addAttribute("membershipRatio", stats.membershipRatio());
    model.addAttribute("newSupporter", stats.newSupporter());
    model.addAttribute("diffNewSupporter", stats.diffNewSupporter());

    model.addAttribute("email", email);
    model.addAttribute("usernameSearch", username);
    model.addAttribute("onlineOnly", onlineOnly);
    model.addAttribute("startDate", startDate);

    return "admin/user-list";
  }

  @GetMapping("/users/{id}")
  public String userDetailPage(@PathVariable int id, Model model) {
    User user = userService.findUserById(id);
    model.addAttribute("user", user);
    return "admin/manage-user";
  }

  @GetMapping("/support")
  public String supportPage(@PageableDefault Pageable pageable, Model model) {
    Page<Inquiry> inquiries = inquiryService.findAll(pageable);
    model.addAttribute("mode", "inquiry");
    model.addAttribute("inquiries", inquiries);
    model.addAttribute("currentPage", pageable.getPageNumber());
    model.addAttribute("totalPages", inquiries.getTotalPages());
    return "admin/admin-support";
  }

  @GetMapping("/contents")
  public String contentPage(@RequestParam(required = false) String mode,
      @PageableDefault Pageable pageable, Model model) {
    model.addAttribute("currentPage", pageable.getPageNumber());
    model.addAttribute("totalPages", 0);
    model.addAttribute("faq", List.of());
    model.addAttribute("notice", List.of());
    model.addAttribute("study", List.of());

    if (mode == null || mode.isBlank() || mode.equals("notice")) {
      model.addAttribute("mode", "notice");
      Page<Post> notices = postService.findAllByPostType(pageable, PostType.NOTICE);
      model.addAttribute("notice", notices);
      model.addAttribute("totalPages", notices.getTotalPages());
    } else if (mode.equals("faq")) {
      model.addAttribute("mode", "faq");
      model.addAttribute("faq", postService.findFAQ());
    } else if (mode.equals("study")) {
      model.addAttribute("mode", "study");
      Page<Post> studies = postService.findAllByPostType(pageable, PostType.STUDY);
      model.addAttribute("study", studies);
      model.addAttribute("totalPages", studies.getTotalPages());
    }

    return "admin/admin-contents";
  }

  @GetMapping("/insight")
  public String insightPage() {
    return "admin/admin-insight";
  }

  @GetMapping("/finance")
  public String finance(@PageableDefault Pageable pageable, Model model) {
    List<Payment> payments = paymentService.findAll(pageable).toList();
    model.addAttribute("payments", payments);
    model.addAttribute("currentPage", pageable.getPageNumber());
    return "admin/admin-finance";
  }

  @GetMapping("/editor")
  public String showEditor() {
    return "admin/notice-post";
  }

  @GetMapping("/posts/{id}")
  public String editPost(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable int id, Model model) {
    model.addAttribute("username", userPrincipal.getUsername());
    model.addAttribute("post", postService.findPostByPostId(id));
    return "admin/notice-post";
  }

  @GetMapping("/inquiries/{id}")
  public String readInquiry(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable int id, Model model) {
    model.addAttribute("username", userPrincipal.getUsername());
    model.addAttribute("isLoggedIn", true);
    model.addAttribute("post", inquiryService.findInquiryById(id));
    return "post-detail";
  }
}
