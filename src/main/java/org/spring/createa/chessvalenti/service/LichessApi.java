package org.spring.createa.chessvalenti.service;

import org.spring.createa.chessvalenti.dto.response.LichessGameResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Flux;

@HttpExchange
public interface LichessApi {

  @GetExchange(value = "games/user/{username}", accept = "application/x-ndjson")
  public Flux<LichessGameResponse> loadGames(@PathVariable String username,
      @RequestParam boolean pgnInJson,
      @RequestParam
      String perfType, @RequestParam String since);
}
