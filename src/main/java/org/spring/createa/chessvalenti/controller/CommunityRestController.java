package org.spring.createa.chessvalenti.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.domain.Inquiry;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.dto.request.InquiryCreateRequest;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.CommentService;
import org.spring.createa.chessvalenti.service.InquiryService;
import org.spring.createa.chessvalenti.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class CommunityRestController {

  private final CommentService commentService;
  private final PostService postService;
  private final InquiryService inquiryService;

  @PostMapping("/posts/{id}/comments")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void saveComment(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable int id, @Valid @RequestBody CommentRequest body) {
    log.info("Saving comment for post {} by user {}", id, userPrincipal.getUsername());
    Post post = postService.findPostByPostId(id);
    if (post.getType() == PostType.FAQ) {
      throw new IllegalArgumentException("Cannot comment on FAQ");
    }
    commentService.saveComment(body.content(), userPrincipal.getUser(), post, body.parentId());
  }

  @DeleteMapping("/comments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN') or @commentService.isOwner(#id, principal.user.userId)")
  public void deleteComment(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable int id) {
    log.info("Deleting comment {} by user {}", id, userPrincipal.getUsername());
    commentService.deleteComment(id);
  }

  @PostMapping("/inquiries")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void createInquiry(@AuthenticationPrincipal UserPrincipal userPrincipal,
      @Valid @RequestBody InquiryCreateRequest body) {
    log.info("Creating inquiry by user: {}", userPrincipal.getUsername());
    inquiryService.save(
        new Inquiry(body.title(), body.content(), userPrincipal.getUser(), body.category()));
  }

  public record CommentRequest(String content, Integer parentId) {

  }
}
