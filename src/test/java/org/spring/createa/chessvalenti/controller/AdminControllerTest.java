package org.spring.createa.chessvalenti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.domain.Difficulty;
import org.spring.createa.chessvalenti.domain.FAQ;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.spring.createa.chessvalenti.domain.Notice;
import org.spring.createa.chessvalenti.domain.Payment;
import org.spring.createa.chessvalenti.domain.Role;
import org.spring.createa.chessvalenti.domain.User;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.dto.request.PatchUserRequest;
import org.spring.createa.chessvalenti.dto.request.PostCreateRequest;
import org.spring.createa.chessvalenti.dto.response.AdminUserStatsResponse;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.InquiryService;
import org.spring.createa.chessvalenti.service.PaymentService;
import org.spring.createa.chessvalenti.service.PostService;
import org.spring.createa.chessvalenti.service.UserService;
import org.spring.createa.chessvalenti.service.FileService;
import org.spring.createa.chessvalenti.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private InquiryService inquiryService;

  @MockBean
  private PaymentService paymentService;

  @MockBean
  private UserService userService;

  @MockBean
  private PostService postService;

  @MockBean
  private FileService fileService;

  @MockBean(name = "timeUtil")
  private TimeUtil timeUtil;

  @Autowired
  private ObjectMapper objectMapper;

  private UserPrincipal adminUser() {
    User user = new User();
    user.setUsername("admin");
    user.setRole(Role.ROLE_ADMIN);
    return new UserPrincipal(user);
  }

  private Authentication adminAuthentication() {
    UserPrincipal principal = adminUser();
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }

  @BeforeEach
  void setUpSecurityContext() {
    SecurityContextHolder.getContext().setAuthentication(adminAuthentication());
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void adminPage_ShouldReturnAdminView() throws Exception {
    mockMvc.perform(get("/admin")
            .with(authentication(adminAuthentication()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/admin"));
  }

  @Test
  void userListPage_ShouldAddStatsToModel() throws Exception {
    AdminUserStatsResponse stats = new AdminUserStatsResponse(
        new PageImpl<>(Collections.emptyList()),
        Collections.emptyList(),
        0, 0, 0, 0.0, 0, 0
    );
    when(userService.getAdminUserStats(any(), any(), anyBoolean(), any(), any())).thenReturn(stats);

    mockMvc.perform(get("/admin/users")
            .with(authentication(adminAuthentication()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("users", "onlineUserCnt"))
        .andExpect(view().name("admin/user-list"));
  }

  @Test
  void userDetailPage_ShouldAddUserToModel() throws Exception {
    User user = new User();
    user.setUsername("target");

    when(userService.findUserById(1)).thenReturn(user);

    mockMvc.perform(get("/admin/users/1")
            .with(authentication(adminAuthentication()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(model().attribute("user", user))
        .andExpect(view().name("admin/manage-user"));
  }

  @Test
  void supportPage_DefaultMode_ShouldAddInquiriesToModel() throws Exception {
    Page<Inquiry> inquiries = new PageImpl<>(Collections.emptyList());

    when(inquiryService.findAll(any())).thenReturn(inquiries);

    mockMvc.perform(get("/admin/support")
            .with(authentication(adminAuthentication()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(model().attribute("mode", "inquiry"))
        .andExpect(model().attribute("inquiries", inquiries))
        .andExpect(view().name("admin/admin-support"));
  }

  @Test
  void supportPage_FaqMode_ShouldAddFaqToModel() throws Exception {
    Post faq = new FAQ();
    faq.setTitle("FAQ");

    when(postService.findFAQ()).thenReturn(Collections.singletonList(faq));

    mockMvc.perform(get("/admin/contents")
            .with(authentication(adminAuthentication()))
            .with(csrf())
            .param("mode", "faq"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("mode", "faq"))
        .andExpect(model().attribute("faq", Collections.singletonList(faq)))
        .andExpect(view().name("admin/admin-contents"));
  }

  @Test
  void supportPage_NoticeMode_ShouldAddNoticesToModel() throws Exception {
    Page<Post> notices = new PageImpl<>(Collections.emptyList());

    when(postService.findAllByPostType(any(), eq(PostType.NOTICE))).thenReturn(notices);

    mockMvc.perform(get("/admin/contents")
            .with(authentication(adminAuthentication()))
            .with(csrf())
            .param("mode", "notice"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("mode", "notice"))
        .andExpect(model().attribute("notice", notices))
        .andExpect(view().name("admin/admin-contents"));
  }

  @Test
  void insightPage_ShouldReturnInsightView() throws Exception {
    mockMvc.perform(get("/admin/insight")
            .with(authentication(adminAuthentication()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/admin-insight"));
  }

  @Test
  void financePage_ShouldAddPaymentsToModel() throws Exception {
    User user = new User();
    user.setUsername("payer");
    Payment payment = new Payment();
    payment.setUser(user);
    Page<Payment> payments = new PageImpl<>(Collections.singletonList(payment));

    when(paymentService.findAll(any())).thenReturn(payments);

    mockMvc.perform(get("/admin/finance")
            .with(authentication(adminAuthentication()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(model().attribute("payments", Collections.singletonList(payment)))
        .andExpect(model().attribute("currentPage", 0))
        .andExpect(view().name("admin/admin-finance"));
  }

  @Test
  void updateUser_ShouldReturnNoContent() throws Exception {
    PatchUserRequest request = new PatchUserRequest("ROLE_ADMIN", false);

    mockMvc.perform(patch("/admin/api/users/1")
            .with(authentication(adminAuthentication()))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    verify(userService).patchUserRoleById(1, "ROLE_ADMIN", false);
  }

  @Test
  void updatePost_ShouldReturnNoContent() throws Exception {
    PostCreateRequest request = new PostCreateRequest("Title", "Content", null, PostType.NOTICE, null, null, null);

    mockMvc.perform(patch("/admin/api/posts/1")
            .with(authentication(adminAuthentication()))
            .with(csrf())
            .param("title", request.title())
            .param("content", request.content()))
        .andExpect(status().isNoContent());

    verify(postService).updatePost(1, "Title", "Content", null, null, null, null);
  }

  @Test
  void deletePost_ShouldReturnNoContent() throws Exception {
    mockMvc.perform(delete("/admin/api/posts/1")
            .with(authentication(adminAuthentication()))
            .with(csrf()))
        .andExpect(status().isNoContent());

    verify(postService).deletePost(1);
  }

  @Test
  void savePost_ShouldReturnNoContent() throws Exception {
    PostCreateRequest request = new PostCreateRequest("Title", "Content", null, PostType.NOTICE, null, null, null);

    mockMvc.perform(post("/admin/api/posts")
            .with(authentication(adminAuthentication()))
            .with(csrf())
            .param("title", request.title())
            .param("content", request.content())
            .param("postType", request.postType().name()))
        .andExpect(status().isNoContent());

    verify(postService).savePost(adminUser().getUser(), "Title", "Content", null, PostType.NOTICE, null, null, null);
  }

  @Test
  void showEditor_ShouldReturnNoticePostView() throws Exception {
    mockMvc.perform(get("/admin/editor")
            .with(authentication(adminAuthentication()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/notice-post"));
  }

  @Test
  void editPost_ShouldAddPostAndUsernameToModel() throws Exception {
    Post post = new Notice();
    post.setTitle("Notice");

    when(postService.findPostByPostId(1)).thenReturn(post);

    mockMvc.perform(get("/admin/posts/1")
            .with(authentication(adminAuthentication()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(model().attribute("username", "admin"))
        .andExpect(model().attribute("post", post))
        .andExpect(view().name("admin/notice-post"));
  }

  @Test
  void readInquiry_ShouldAddInquiryAndUsernameToModel() throws Exception {
    Inquiry inquiry = new Inquiry();
    inquiry.setTitle("Question");
    User writer = new User();
    writer.setUsername("writer");
    inquiry.setWriter(writer);

    when(inquiryService.findInquiryById(1)).thenReturn(inquiry);

    mockMvc.perform(get("/admin/inquiries/1")
            .with(authentication(adminAuthentication()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(model().attribute("username", "admin"))
        .andExpect(model().attribute("post", inquiry))
        .andExpect(view().name("post-detail"));
  }
}
