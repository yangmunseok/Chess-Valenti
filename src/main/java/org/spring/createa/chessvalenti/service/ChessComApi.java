package org.spring.createa.chessvalenti.service;

import org.spring.createa.chessvalenti.dto.response.ChessComArchivesResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;

@HttpExchange
public interface ChessComApi {

  @GetExchange("player/{username}/games/archives/")
  Mono<ChessComArchivesResponse> loadArchives(@PathVariable String username);

  @GetExchange("player/{username}/games/{year}/{month}/pgn")
  Mono<String> loadMonthlyPgn(@PathVariable String username, @PathVariable String year,
      @PathVariable String month);
}
