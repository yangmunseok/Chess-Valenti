package org.spring.createa.chessvalenti.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.spring.createa.chessvalenti.db.CommentRepository;
import org.spring.createa.chessvalenti.domain.Comment;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;

  public List<Comment> findCommentsByPost(Post post) {
    return commentRepository.findAllByPostAndParentIsNullOrderByCreatedAtAsc(post);
  }

  public long countCommentsByPost(Post post) {
    return commentRepository.countByPost(post);
  }

  @Transactional
  public Comment saveComment(String content, User writer, Post post, Integer parentId) {
    Comment parent = null;
    if (parentId != null) {
      parent = commentRepository.findById(parentId).orElse(null);
    }
    Comment comment = new Comment(content, writer, post, parent);
    return commentRepository.save(comment);
  }

  @Transactional
  public void deleteComment(int id) {
    commentRepository.deleteById(id);
  }
  
  public Comment findById(int id) {
    return commentRepository.findById(id).orElseThrow();
  }
}
