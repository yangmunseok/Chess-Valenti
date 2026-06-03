package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
public class ChessPlayer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  long id;

  @jakarta.persistence.Column(unique = true)
  String name;

  public ChessPlayer() {

  }

  public ChessPlayer(String name) {
    this.name = name;
  }
}
