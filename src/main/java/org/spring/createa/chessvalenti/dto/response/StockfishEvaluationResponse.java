package org.spring.createa.chessvalenti.dto.response;

import java.util.List;

public record StockfishEvaluationResponse(List<PrincipalVariation> pvs) {

  public record PrincipalVariation(Integer cp, Integer mate) {
  }
}
