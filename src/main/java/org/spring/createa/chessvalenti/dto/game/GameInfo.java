package org.spring.createa.chessvalenti.dto.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.game.GameResult;
import com.github.bhlangonijr.chesslib.game.Player;
import java.util.Map;


public class GameInfo {

  @JsonIgnore
  Game game;

  String pgn;
  String initial_fen;
  long moveIdx;
  long gameOffset;

  public String getDate() {
    return date;
  }

  public Player getWhitePlayer() {
    return whitePlayer;
  }

  public Player getBlackPlayer() {
    return blackPlayer;
  }

  public GameResult getResult() {
    return result;
  }

  public Map<String, String> getProperty() {
    return property;
  }

  public String getFen() {
    return fen;
  }

  public String getOpening() {
    return opening;
  }

  private String date;
  private Player whitePlayer;
  private Player blackPlayer;
  private GameResult result;
  private Map<String, String> property;
  private String fen;
  private String opening;
  private String event;

  public String getEvent() {
    return event;
  }

  public Game getGame() {
    return game;
  }

  public void setGame(Game game) {
    this.game = game;
    this.date = game.getDate();
    this.whitePlayer = game.getWhitePlayer();
    this.blackPlayer = game.getBlackPlayer();
    this.result = game.getResult();
    this.property = game.getProperty();
    this.fen = game.getFen();
    this.opening = game.getOpening();
    if (game.getRound() != null && game.getRound().getEvent() != null) {
      this.event = game.getRound().getEvent().getName();
    }
  }

  public String getPgn() {
    return pgn;
  }

  public void setPgn(String pgn) {
    this.pgn = pgn;
  }

  public String getInitial_fen() {
    return initial_fen;
  }

  public void setInitial_fen(String initial_fen) {
    this.initial_fen = initial_fen;
  }

  public long getMoveIdx() {
    return moveIdx;
  }

  public void setMoveIdx(long moveIdx) {
    this.moveIdx = moveIdx;
  }

  public long getGameOffset() {
    return gameOffset;
  }

  public void setGameOffset(long gameOffset) {
    this.gameOffset = gameOffset;
  }

}
