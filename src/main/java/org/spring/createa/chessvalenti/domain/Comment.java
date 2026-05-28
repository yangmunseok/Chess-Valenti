package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class Comment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @CreatedDate
  private LocalDateTime createdAt;

  @ManyToOne
  private User writer;

  @ManyToOne
  private Post post;

  @ManyToOne
  private Comment parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  private java.util.List<Comment> children = new java.util.ArrayList<>();

  public Comment() {
  }

  public Comment(String content, User writer, Post post, Comment parent) {
    this.content = content;
    this.writer = writer;
    this.post = post;
    this.parent = parent;
  }
}
