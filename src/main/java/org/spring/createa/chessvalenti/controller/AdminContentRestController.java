package org.spring.createa.chessvalenti.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.domain.Difficulty;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.FileService;
import org.spring.createa.chessvalenti.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin Content REST API", description = "관리자 전용 게시글(학습 자료, FAQ 등) 관리 API")
@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminContentRestController {

    private final PostService postService;
    private final FileService fileService;

    @Operation(summary = "게시글 작성", description = "새로운 게시글(학습 자료, FAQ 등)을 작성합니다. 이미지를 업로드할 수 있습니다.")
    @PostMapping
    public ResponseEntity<Void> savePost(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "게시글 제목") @RequestParam("title") String title,
            @Parameter(description = "게시글 내용") @RequestParam("content") String content,
            @Parameter(description = "비디오 링크 (유튜브 등)") @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @Parameter(description = "게시글 타입 (STUDY, FAQ, POST 등)") @RequestParam("postType") PostType postType,
            @Parameter(description = "난이도 (BEGINNER, INTERMEDIATE, ADVANCED)") @RequestParam(value = "difficulty", required = false) Difficulty difficulty,
            @Parameter(description = "게시글 한 줄 소개") @RequestParam(value = "introduction", required = false) String introduction,
            @Parameter(description = "대표 이미지 파일") @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        log.info("Saving post by user {}", userPrincipal.getUsername());
        String imageUrl = (imageFile != null && !imageFile.isEmpty()) ? fileService.saveFile(imageFile) : null;
        postService.savePost(userPrincipal.getUser(), title, content, videoUrl, postType, difficulty, introduction, imageUrl);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 수정", description = "기존 게시글의 내용을 수정합니다. 이미지도 변경할 수 있습니다.")
    @PatchMapping("/{id}")
    public ResponseEntity<Void> updatePost(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "수정할 게시글 ID") @PathVariable int id,
            @Parameter(description = "게시글 제목") @RequestParam("title") String title,
            @Parameter(description = "게시글 내용") @RequestParam("content") String content,
            @Parameter(description = "비디오 링크") @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @Parameter(description = "난이도") @RequestParam(value = "difficulty", required = false) Difficulty difficulty,
            @Parameter(description = "게시글 한 줄 소개") @RequestParam(value = "introduction", required = false) String introduction,
            @Parameter(description = "대표 이미지 파일") @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        log.info("Updating post {} by user {}", id, userPrincipal.getUsername());
        String imageUrl = (imageFile != null && !imageFile.isEmpty()) ? fileService.saveFile(imageFile) : null;
        postService.updatePost(id, title, content, videoUrl, difficulty, introduction, imageUrl);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 삭제", description = "특정 게시글을 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "삭제할 게시글 ID") @PathVariable int id) {
        log.info("Deleting post {} by user {}", id, userPrincipal.getUsername());
        postService.deletePost(id);
    }
}
