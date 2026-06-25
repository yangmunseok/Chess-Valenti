package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "post_type", discriminatorType = DiscriminatorType.STRING)
@Table(name = "post", uniqueConstraints = {
    @UniqueConstraint(name = "uk_post_title", columnNames = "title")
})
@Data
@ToString(exclude = {"writer", "comments"})
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

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  List<Comment> comments = new ArrayList<>();

  public String getPlainTextContent() {
    if (content == null) {
      return "";
    }
    // Remove HTML tags using regex
    return content.replaceAll("<[^>]*>", "");
  }
}
