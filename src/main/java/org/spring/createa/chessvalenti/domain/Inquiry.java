package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Column;
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
public class Inquiry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @CreatedDate
  LocalDateTime createdAt;
  LocalDateTime updatedAt;

  String title;
  @Column(columnDefinition = "TEXT")
  String content;

  @ManyToOne
  User writer;

  @Enumerated(EnumType.STRING)
  InquiryCategory category;

  @Column(columnDefinition = "TEXT")
  String answer;
  LocalDateTime answeredAt;

  public Inquiry() {

  }

  public Inquiry(String title, String content, User writer, InquiryCategory category) {
    this.title = title;
    this.content = content;
    this.writer = writer;
    this.category = category;
  }
}