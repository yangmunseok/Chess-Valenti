package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int postId;

  @CreatedDate
  LocalDateTime createdAt;
  LocalDateTime updatedAt;

  String title;
  String content;
  String videoUrl;

  @ManyToOne
  User writer;

  int view;

  @Enumerated(EnumType.STRING)
  PostType type;

}
