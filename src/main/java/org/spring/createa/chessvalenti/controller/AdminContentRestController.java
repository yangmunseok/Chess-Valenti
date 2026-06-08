package org.spring.createa.chessvalenti.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.domain.Difficulty;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.security.UserPrincipal;
import org.spring.createa.chessvalenti.service.FileService;
import org.spring.createa.chessvalenti.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminContentRestController {

    private final PostService postService;
    private final FileService fileService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void savePost(@AuthenticationPrincipal UserPrincipal userPrincipal,
                         @RequestParam("title") String title,
                         @RequestParam("content") String content,
                         @RequestParam(value = "videoUrl", required = false) String videoUrl,
                         @RequestParam("postType") PostType postType,
                         @RequestParam(value = "difficulty", required = false) Difficulty difficulty,
                         @RequestParam(value = "introduction", required = false) String introduction,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        log.info("Saving post by user {}", userPrincipal.getUsername());
        String imageUrl = (imageFile != null && !imageFile.isEmpty()) ? fileService.saveFile(imageFile) : null;
        postService.savePost(userPrincipal.getUser(), title, content, videoUrl, postType, difficulty, introduction, imageUrl);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePost(@AuthenticationPrincipal UserPrincipal userPrincipal,
                           @PathVariable int id,
                           @RequestParam("title") String title,
                           @RequestParam("content") String content,
                           @RequestParam(value = "videoUrl", required = false) String videoUrl,
                           @RequestParam(value = "difficulty", required = false) Difficulty difficulty,
                           @RequestParam(value = "introduction", required = false) String introduction,
                           @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        log.info("Updating post {} by user {}", id, userPrincipal.getUsername());
        String imageUrl = (imageFile != null && !imageFile.isEmpty()) ? fileService.saveFile(imageFile) : null;
        postService.updatePost(id, title, content, videoUrl, difficulty, introduction, imageUrl);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable int id) {
        log.info("Deleting post {} by user {}", id, userPrincipal.getUsername());
        postService.deletePost(id);
    }
}
