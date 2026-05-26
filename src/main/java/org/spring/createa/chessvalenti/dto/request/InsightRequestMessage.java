package org.spring.createa.chessvalenti.dto.request;

public record InsightRequestMessage(String username, String perfType, String since,
                                    Boolean cancel, Long id, String platform) {

  public InsightRequestMessage(String username, String perfType, String since,
      Boolean cancel, Long id) {
    this(username, perfType, since, cancel, id, "lichess");
  }

}
