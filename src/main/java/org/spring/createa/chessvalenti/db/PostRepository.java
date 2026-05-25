package org.spring.createa.chessvalenti.db;

import java.util.List;
import org.spring.createa.chessvalenti.domain.Post;
import org.spring.createa.chessvalenti.domain.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Integer> {

  Page<Post> findAllByType(Pageable pageable, PostType postType);

  List<Post> findAllByType(PostType postType);


  Post findPostsByPostId(int id);

  void deletePostByPostId(int id);

  void deletePostsByPostId(int postId);
}
