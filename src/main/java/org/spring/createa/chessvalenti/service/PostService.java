package org.spring.createa.chessvalenti.service;

import java.util.List;
import org.spring.createa.chessvalenti.db.PostRepository;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.PostType;
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

  PostRepository postRepository;

  public PostService(PostRepository postRepository) {
    this.postRepository = postRepository;
  }

  public Post savePost(User writer, String title, String content, PostType postType) {
    Post post = new Post();
    post.setWriter(writer);
    post.setTitle(title);
    post.setContent(content);
    post.setType(postType);
    return savePost(post);
  }

  public Post savePost(Post post) {
    return postRepository.save(post);
  }

  @PreAuthorize("@postService.isOwner(#postId, authentication.name)")
  public Post updatePost(int postId, String title, String content) {
    Post post = postRepository.findPostsByPostId(postId);
    if (title != null) {
      post.setTitle(title);
    }
    if (content != null) {
      post.setContent(content);
    }
    return postRepository.save(post);
  }

  @Transactional
  public void deletePost(int id) {
    postRepository.deletePostsByPostId(id);
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




