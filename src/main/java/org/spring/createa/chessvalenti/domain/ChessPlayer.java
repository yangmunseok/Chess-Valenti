package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ChessPlayer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  long id;

  String playerId;
  String name;
  String description;

  public ChessPlayer() {

  }

  public ChessPlayer(String playerId, String name, String description) {
    this.playerId = playerId;
    this.name = name;
    this.description = description;
  }
}
