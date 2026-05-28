package org.spring.createa.chessvalenti.db;

import java.util.List;
import org.spring.createa.chessvalenti.domain.Comment;
import org.spring.createa.chessvalenti.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
  List<Comment> findAllByPostAndParentIsNullOrderByCreatedAtAsc(Post post);
  
  long countByPost(Post post);
}
