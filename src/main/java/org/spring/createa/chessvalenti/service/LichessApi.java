package org.spring.createa.chessvalenti.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.spring.createa.chessvalenti.dto.response.LichessGameResponse;
import org.spring.createa.chessvalenti.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class LichessApi {

  private static final MediaType NDJSON = MediaType.parseMediaType("application/x-ndjson");

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public LichessApi(RestClient restClient, ObjectMapper objectMapper) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
  }

  /**
   * 게임을 한 줄(ndjson)씩 읽어 consumer에 넘긴다. 메모리에는 항상 게임 한 판만 올라간다.
   */
  public void loadGames(String username, String perfType, String since, JobHandle handle,
      Consumer<LichessGameResponse> consumer) {
    restClient.get()
        .uri(builder -> builder.path("games/user/{username}")
            .queryParam("pgnInJson", true)
            .queryParam("perfType", perfType)
            .queryParam("since", since)
            .build(username))
        .accept(NDJSON)
        .exchange((request, response) -> {
          if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            throw new UserNotFoundException("존재하지 않는 사용자입니다: " + username);
          }
          if (response.getStatusCode().isError()) {
            throw new IllegalStateException("Lichess API error: " + response.getStatusCode());
          }

          InputStream body = response.getBody();
          handle.attachStream(body);
          BufferedReader reader = new BufferedReader(
              new InputStreamReader(body, StandardCharsets.UTF_8));
          String line;
          while (!handle.isCancelled() && (line = reader.readLine()) != null) {
            if (!line.isBlank()) {
              consumer.accept(objectMapper.readValue(line, LichessGameResponse.class));
            }
          }
          return null;
        });
  }
}
