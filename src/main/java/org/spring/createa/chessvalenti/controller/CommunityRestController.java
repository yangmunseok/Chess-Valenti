package org.spring.createa.chessvalenti.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Community REST API", description = "커뮤니티 댓글 및 문의사항 관리 API")
@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class CommunityRestController {

  private final CommentService commentService;
  private final PostService postService;
  private final InquiryService inquiryService;

  @Operation(summary = "댓글 등록", description = "특정 게시글에 새로운 댓글(또는 대댓글)을 작성합니다.")
  @PostMapping("/posts/{id}/comments")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void saveComment(
      @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Parameter(description = "게시글 ID") @PathVariable int id,
      @Valid @RequestBody CommentRequest body) {
    log.info("Saving comment for post {} by user {}", id, userPrincipal.getUsername());
    Post post = postService.findPostByPostId(id);
    if (post.getType() == PostType.FAQ) {
      throw new IllegalArgumentException("Cannot comment on FAQ");
    }
    commentService.saveComment(body.content(), userPrincipal.getUser(), post, body.parentId());
  }

  @Operation(summary = "댓글 삭제", description = "특정 댓글을 삭제합니다. 관리자 혹은 본인이 작성한 댓글만 삭제할 수 있습니다.")
  @DeleteMapping("/comments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN') or @commentService.isOwner(#id, principal.user.userId)")
  public void deleteComment(
      @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Parameter(description = "댓글 ID") @PathVariable int id) {
    log.info("Deleting comment {} by user {}", id, userPrincipal.getUsername());
    commentService.deleteComment(id);
  }

  @Operation(summary = "1:1 문의사항 생성", description = "로그인한 사용자가 새로운 1:1 문의사항을 등록합니다.")
  @PostMapping("/inquiries")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void createInquiry(
      @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Valid @RequestBody InquiryCreateRequest body) {
    log.info("Creating inquiry by user: {}", userPrincipal.getUsername());
    inquiryService.save(
        new Inquiry(body.title(), body.content(), userPrincipal.getUser(), body.category()));
  }

  public record CommentRequest(
      @Schema(description = "댓글 내용") String content,
      @Schema(description = "부모 댓글 ID (대댓글인 경우)") Integer parentId) {

  }
}
