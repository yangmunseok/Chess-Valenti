package org.spring.createa.chessvalenti.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.createa.chessvalenti.db.PostRepository;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.PostType;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

  @Mock
  private PostRepository postRepository;

  @InjectMocks
  private PostService postService;

  private User writer;

  @BeforeEach
  void setUp() {
    writer = new User();
    writer.setUserId(1);
    writer.setUsername("testuser");
  }

  @Test
  void savePost_WithDetails_ShouldSaveAndReturnPost() {
    Post post = new Post();
    post.setWriter(writer);
    post.setTitle("Title");
    post.setContent("Content");
    post.setType(PostType.NOTICE);

    when(postRepository.save(any(Post.class))).thenReturn(post);

    Post saved = postService.savePost(writer, "Title", "Content", PostType.NOTICE);

    assertNotNull(saved);
    assertEquals("Title", saved.getTitle());
    assertEquals(writer, saved.getWriter());
    verify(postRepository).save(any(Post.class));
  }

  @Test
  void updatePost_ShouldUpdateFieldsAndSave() {
    Post post = new Post();
    post.setPostId(1);
    post.setTitle("Old Title");
    post.setContent("Old Content");

    when(postRepository.findPostsByPostId(1)).thenReturn(post);
    when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArguments()[0]);

    Post updated = postService.updatePost(1, "New Title", "New Content");

    assertEquals("New Title", updated.getTitle());
    assertEquals("New Content", updated.getContent());
    verify(postRepository).save(post);
  }

  @Test
  void deletePost_ShouldCallRepositoryDelete() {
    postService.deletePost(1);
    verify(postRepository).deletePostsByPostId(1);
  }

  @Test
  void findAllByPostType_ShouldReturnPage() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Post> page = new PageImpl<>(List.of(new Post()));
    when(postRepository.findAllByType(any(Pageable.class), eq(PostType.NOTICE))).thenReturn(page);

    Page<Post> result = postService.findAllByPostType(pageable, PostType.NOTICE);

    assertFalse(result.isEmpty());
    verify(postRepository).findAllByType(any(Pageable.class), eq(PostType.NOTICE));
  }

  @Test
  void findFAQ_ShouldReturnList() {
    when(postRepository.findAllByType(PostType.FAQ)).thenReturn(List.of(new Post()));
    List<Post> faqs = postService.findFAQ();
    assertFalse(faqs.isEmpty());
  }

  @Test
  void isOwner_WhenIsOwner_ShouldReturnTrue() {
    Post post = new Post();
    post.setWriter(writer);
    when(postRepository.findById(1)).thenReturn(Optional.of(post));

    assertTrue(postService.isOwner(1, "testuser"));
  }

  @Test
  void isOwner_WhenIsNotOwner_ShouldReturnFalse() {
    Post post = new Post();
    post.setWriter(writer);
    when(postRepository.findById(1)).thenReturn(Optional.of(post));

    assertFalse(postService.isOwner(1, "otheruser"));
  }
}
