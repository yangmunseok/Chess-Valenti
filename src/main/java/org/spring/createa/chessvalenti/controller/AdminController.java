package org.spring.createa.chessvalenti.controller;

import java.time.LocalDate;
import java.util.List;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.spring.createa.chessvalenti.domain.Payment;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.dto.request.PostCreateRequest;
import org.spring.createa.chessvalenti.dto.request.PatchUserRequest;
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
public class AdminController {

  private final InquiryService inquiryService;
  PaymentService paymentService;
  UserService userService;
  PostService postService;

  public AdminController(PaymentService paymentService, UserService userService,
      PostService postService, InquiryService inquiryService) {
    this.paymentService = paymentService;
    this.userService = userService;
    this.postService = postService;
    this.inquiryService = inquiryService;
  }

  @GetMapping("")
  public String adminPage() {
    return "admin/admin";
  }

  @GetMapping("/users")
  public String userListPage(@PageableDefault Pageable pageable,
      @RequestParam(required = false) String email,
      Model model) {
    Page<User> users = (email == null || email.isBlank()) ? userService.findAll(pageable)
        : userService.findAllByEmail(email, pageable);
    model.addAttribute("users", users);

    List<User> loginUsers = users.filter(user -> userService.isUserOnline(user.getUsername()))
        .toList();

    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    LocalDate lastMonth = today.minusMonths(1);
    int newUserCnt = userService.countUsersByCreationDate(today.getYear(), today.getMonthValue(),
        today.getDayOfMonth());
    int yesterdayNewUserCnt = userService.countUsersByCreationDate(yesterday.getYear(),
        yesterday.getMonthValue(),
        yesterday.getDayOfMonth());
    int newSupporter = userService.countUsersByCreationMonthAndDonationNot(
        today.getYear(),
        today.getMonthValue(),
        0);
    int lastMonthSupporter = userService.countUsersByCreationMonthAndDonationNot(
        lastMonth.getYear(), lastMonth.getMonthValue(), 0);

    model.addAttribute("loginUsers", loginUsers);
    model.addAttribute("totalPages", users.getTotalPages());
    model.addAttribute("currentPage", pageable.getPageNumber());
    model.addAttribute("newUsersCnt", newUserCnt);
    model.addAttribute("diffNewUser", newUserCnt - yesterdayNewUserCnt);
    model.addAttribute("onlineUserCnt", userService.onlineUsersCnt());
    model.addAttribute("membershipRatio", userService.getMemberShipRatio());
    model.addAttribute("newSupporter", newSupporter);
    model.addAttribute("diffNewSupporter", newSupporter - lastMonthSupporter);
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

  @PatchMapping("/api/users/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateUser(@PathVariable int id, @RequestBody PatchUserRequest body) {
    System.out.println(id);
    userService.patchUserRoleById(id, body.role(), body.ban());
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PatchMapping("/api/posts/{id}")
  public void updatePost(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PostCreateRequest body, @PathVariable int id) {
    postService.updatePost(id, body.title(), body.content());
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/api/posts/{id}")
  public void deletePost(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable int id) {
    postService.deletePost(id);
  }


  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/api/posts")
  public void savePost(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PostCreateRequest body) {
    postService.savePost(userPrincipal.getUser(), body.title(), body.content(),
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
