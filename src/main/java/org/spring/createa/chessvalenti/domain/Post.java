package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "post_type", discriminatorType = DiscriminatorType.STRING)
@Data
public abstract class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int postId;

  @CreatedDate
  LocalDateTime createdAt;
  LocalDateTime updatedAt;

  @Column(unique = true)
  String title;

  @Column(columnDefinition = "TEXT")
  String content;

  @ManyToOne
  User writer;

  int view;

  @Enumerated(EnumType.STRING)
  @Column(name = "post_type", insertable = false, updatable = false)
  PostType type;

  public String getPlainTextContent() {
    if (content == null) {
      return "";
    }
    // Remove HTML tags using regex
    return content.replaceAll("<[^>]*>", "");
  }
}
