package org.spring.createa.chessvalenti.service;

import java.util.List;
import org.spring.createa.chessvalenti.db.PostRepository;
import org.spring.createa.chessvalenti.domain.Difficulty;
import org.spring.createa.chessvalenti.domain.FAQ;
import org.spring.createa.chessvalenti.domain.Notice;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.domain.Study;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

  private final PostRepository postRepository;

  public PostService(PostRepository postRepository) {
    this.postRepository = postRepository;
  }

  public Post savePost(User writer, String title, String content, String videoUrl,
      PostType postType, Difficulty difficulty, String introduction, String imageUrl) {
    Post post;
    switch (postType) {
      case STUDY -> {
        Study study = new Study();
        study.setVideoUrl(videoUrl);
        study.setDifficulty(difficulty);
        study.setIntroduction(introduction);
        study.setImageUrl(imageUrl);
        post = study;
      }
      case FAQ -> post = new FAQ();
      case NOTICE -> post = new Notice();
      default -> throw new IllegalArgumentException("Unsupported post type: " + postType);
    }

    post.setWriter(writer);
    post.setTitle(title);
    post.setContent(content);
    return savePost(post);
  }

  public Post savePost(Post post) {
    return postRepository.save(post);
  }

  @PreAuthorize("@postService.isOwner(#postId, authentication.name)")
  public Post updatePost(int postId, String title, String content, String videoUrl,
      Difficulty difficulty, String introduction, String imageUrl) {
    Post post = postRepository.findPostsByPostId(postId);
    if (title != null) {
      post.setTitle(title);
    }
    if (content != null) {
      post.setContent(content);
    }

    if (post instanceof Study study) {
      if (videoUrl != null) {
        study.setVideoUrl(videoUrl);
      }
      if (difficulty != null) {
        study.setDifficulty(difficulty);
      }
      if (introduction != null) {
        study.setIntroduction(introduction);
      }
      if (imageUrl != null) {
        study.setImageUrl(imageUrl);
      }
    }

    return postRepository.save(post);
  }

  @Transactional
  public void deletePost(int id) {
    Post post = postRepository.findById(id).orElseThrow();
    postRepository.delete(post);
  }

  public Page<Post> findAllByPostType(Pageable pageable, PostType postType) {
    Sort sort = Sort.by(Sort.Order.desc("createdAt"));
    PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        sort);
    return postRepository.findAllByType(pageRequest, postType);
  }

  public Post findPostByPostId(int id) {
    return postRepository.findPostsByPostId(id);
  }

  public List<Post> findFAQ() {
    return postRepository.findAllByType(PostType.FAQ);
  }

  public boolean isOwner(int postId, String username) {
    Post post = postRepository.findById(postId)
        .orElseThrow();

    return post.getWriter().getUsername().equals(username);
  }
}
