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
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@ToString(exclude = {"user"})
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  long paymentId;

  String product;
  int amount;
  String method;
  @CreatedDate
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @Enumerated(EnumType.STRING)
  PaymentState state;

  @ManyToOne
  User user;
}
