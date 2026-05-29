package org.spring.createa.chessvalenti.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
  @ManyToOne
  ChessPlayer whitePlayer;
  @ManyToOne
  ChessPlayer blackPlayer;
  int whiteElo;
  int blackElo;

  public GameIndex() {

  }

  public GameIndex(long pawnStructure, int pieceConfiguration, long gameOffset, long moveIndex) {
    this(pawnStructure, pieceConfiguration, gameOffset, moveIndex, null, null);
  }

  public GameIndex(long pawnStructure, int pieceConfiguration, long gameOffset, long moveIndex,
      ChessPlayer whitePlayer, ChessPlayer blackPlayer) {
    this(pawnStructure, pieceConfiguration, gameOffset, moveIndex, whitePlayer, blackPlayer, 0, 0);
  }

  public GameIndex(long pawnStructure, int pieceConfiguration, long gameOffset, long moveIndex,
      ChessPlayer whitePlayer, ChessPlayer blackPlayer, int whiteElo, int blackElo) {
    this.pawnStructure = pawnStructure;
    this.pieceConfiguration = pieceConfiguration;
    this.gameOffset = gameOffset;
    this.moveIndex = moveIndex;
    this.whitePlayer = whitePlayer;
    this.blackPlayer = blackPlayer;
    this.whiteElo = whiteElo;
    this.blackElo = blackElo;
  }

  long moveIndex;
}
