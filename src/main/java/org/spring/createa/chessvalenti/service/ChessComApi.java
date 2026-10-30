package org.spring.createa.chessvalenti.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.spring.createa.chessvalenti.dto.response.ChessComArchivesResponse;
import org.spring.createa.chessvalenti.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

public class ChessComApi {

  private static final String GAME_START = "[Event \"";

  private final RestClient restClient;

  public ChessComApi(RestClient restClient) {
    this.restClient = restClient;
  }

  public ChessComArchivesResponse loadArchives(String username) {
    return restClient.get()
        .uri("player/{username}/games/archives/", username)
        .retrieve()
        .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
          throw new UserNotFoundException("존재하지 않는 사용자입니다: " + username);
        })
        .body(ChessComArchivesResponse.class);
  }

  /**
   * 한 달치 PGN을 줄 단위로 읽다가 게임 하나가 끝날 때마다 consumer에 넘긴다.
   * 한 달 전체를 문자열 하나로 들고 있지 않는다.
   */
  public void loadMonthlyPgn(String username, String year, String month, CancellationToken token,
      Consumer<String> consumer) {
    restClient.get()
        .uri("player/{username}/games/{year}/{month}/pgn", username, year, month)
        .exchange((request, response) -> {
          if (response.getStatusCode().isError()) {
            throw new IllegalStateException("Chess.com API error: " + response.getStatusCode());
          }

          InputStream body = response.getBody();
          token.onCancel(body);
          BufferedReader reader = new BufferedReader(
              new InputStreamReader(body, StandardCharsets.UTF_8));
          StringBuilder game = new StringBuilder();
          String line;
          while (!token.isCancelled() && (line = reader.readLine()) != null) {
            if (line.startsWith(GAME_START) && !game.isEmpty()) {
              emit(game, consumer);
            }
            game.append(line).append('\n');
          }
          if (!token.isCancelled()) {
            emit(game, consumer);
          }
          return null;
        });
  }

  private void emit(StringBuilder game, Consumer<String> consumer) {
    String pgn = game.toString().trim();
    game.setLength(0);
    if (!pgn.isEmpty()) {
      consumer.accept(pgn);
    }
  }
}
