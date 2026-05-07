package org.spring.createa.chessvalenti.dto.response;

public record LichessGameResponse(String winner, String pgn, String moves, LichessPlayers players,
                                  String variant) {

  public record LichessPlayers(LichessPlayerSide white, LichessPlayerSide black) {

  }

  public record LichessPlayerSide(LichessUser user) {

  }

  public record LichessUser(String name, String id) {

  }
}
