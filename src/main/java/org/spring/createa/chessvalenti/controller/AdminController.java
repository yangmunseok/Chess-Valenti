package org.spring.createa.chessvalenti.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.spring.createa.chessvalenti.domain.Payment;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.request.PostCreateRequest;
import org.spring.createa.chessvalenti.dto.request.PatchUserRequest;
import org.spring.createa.chessvalenti.dto.response.AdminUserStatsResponse;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.InquiryService;
import org.spring.createa.chessvalenti.service.PaymentService;
import org.spring.createa.chessvalenti.service.PostService;
import org.spring.createa.chessvalenti.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

  private final InquiryService inquiryService;
  private final PaymentService paymentService;
  private final UserService userService;
  private final PostService postService;

  @GetMapping("")
  public String adminPage() {
    return "admin/admin";
  }

  @GetMapping("/users")
  public String userListPage(@PageableDefault Pageable pageable,
      @RequestParam(required = false) String email,
      Model model) {
    AdminUserStatsResponse stats = userService.getAdminUserStats(email, pageable);

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

    return "admin/user-list";
  }

  @GetMapping("/users/{id}")
  public String userDetailPage(@PathVariable int id, Model model) {
    User user = userService.findUserById(id);
    model.addAttribute(user);
    return "admin/manage-user";
  }

  @GetMapping("/support")
  public String supportPage(@RequestParam(required = false) String mode,
      @PageableDefault Pageable pageable, Model model) {
    model.addAttribute("currentPage", pageable.getPageNumber());
    model.addAttribute("totalPages", 0);
    model.addAttribute("faq", List.of());
    model.addAttribute("inquiries", List.of());
    if (mode == null || mode.isBlank() || mode.equals("inquiry")) {
      Page<Inquiry> inquiries = inquiryService.findAll(pageable);
      model.addAttribute("mode", "inquiry");
      model.addAttribute("inquiries", inquiries);
      model.addAttribute("totalPages", inquiries.getTotalPages());
    } else if (mode.equals("faq")) {
      model.addAttribute("mode", "faq");
      model.addAttribute("faq", postService.findFAQ());
    } else if (mode.equals("notice")) {
      model.addAttribute("mode", "notice");
      Page<Post> notices = postService.findAllByPostType(pageable, PostType.NOTICE);

      model.addAttribute("notice", notices);
      model.addAttribute("totalPages", notices.getTotalPages());
    }

    return "admin/admin-support";
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

  // Recommendation: Move to /api/admin/users/{id}
  @PatchMapping("/api/users/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateUser(@PathVariable int id, @RequestBody PatchUserRequest body) {
    log.info("Updating user {} with body: {}", id, body);
    userService.patchUserRoleById(id, body.role(), body.ban());
  }

  // Recommendation: Move to /api/admin/posts/{id}
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PatchMapping("/api/posts/{id}")
  public void updatePost(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PostCreateRequest body, @PathVariable int id) {
    log.info("Updating post {} by user {}", id, userPrincipal.getUsername());
    postService.updatePost(id, body.title(), body.content(), body.videoUrl());
  }

  // Recommendation: Move to /api/admin/posts/{id}
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/api/posts/{id}")
  public void deletePost(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable int id) {
    log.info("Deleting post {} by user {}", id, userPrincipal.getUsername());
    postService.deletePost(id);
  }

  // Recommendation: Move to /api/admin/posts
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/api/posts")
  public void savePost(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PostCreateRequest body) {
    log.info("Saving post by user {}", userPrincipal.getUsername());
    postService.savePost(userPrincipal.getUser(), body.title(), body.content(), body.videoUrl(),
        body.postType());
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
    model.addAttribute("post", inquiryService.findInquiryById(id));
    return "post-detail";
  }

}
