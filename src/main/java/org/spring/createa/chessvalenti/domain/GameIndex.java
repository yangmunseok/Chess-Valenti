package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;


@Entity
@Data
public class GameIndex {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_seq")
  @SequenceGenerator(name = "game_seq", sequenceName = "game_index_seq", allocationSize = 1000)
  long id;

  long pawnStructure;
  int pieceConfiguration;
  long gameOffset;

  public GameIndex() {

  }

  public GameIndex(long pawnStructure, int pieceConfiguration, long gameOffset, long moveIndex) {
    this.pawnStructure = pawnStructure;
    this.pieceConfiguration = pieceConfiguration;
    this.gameOffset = gameOffset;
    this.moveIndex = moveIndex;
  }

  long moveIndex;
}
