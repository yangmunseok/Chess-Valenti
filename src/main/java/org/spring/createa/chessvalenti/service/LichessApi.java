package org.spring.createa.chessvalenti.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.spring.createa.chessvalenti.dto.response.LichessGameResponse;
import org.spring.createa.chessvalenti.exception.RateLimitedException;
import org.spring.createa.chessvalenti.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Lichess API 규칙(https://lichess.org/api)을 지킨다.
 * <ul>
 *   <li>요청은 한 번에 하나만 보낸다. 다른 분석의 요청은 들어온 순서대로 기다린다.</li>
 *   <li>429를 받으면 1분을 통째로 쉰 뒤 다시 시도한다. 쉬는 동안 다른 요청도 보내지 않는다.</li>
 * </ul>
 */
@Slf4j
public class LichessApi {

  private static final MediaType NDJSON = MediaType.parseMediaType("application/x-ndjson");
  private static final Duration DEFAULT_RATE_LIMIT_BACKOFF = Duration.ofMinutes(1);
  private static final int MAX_RATE_LIMIT_RETRIES = 2;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final Duration rateLimitBackoff;
  // fair: 기다린 순서대로 차례를 준다
  private final ReentrantLock requestLock = new ReentrantLock(true);

  public LichessApi(RestClient restClient, ObjectMapper objectMapper) {
    this(restClient, objectMapper, DEFAULT_RATE_LIMIT_BACKOFF);
  }

  LichessApi(RestClient restClient, ObjectMapper objectMapper, Duration rateLimitBackoff) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.rateLimitBackoff = rateLimitBackoff;
  }

  /**
   * 게임을 한 줄(ndjson)씩 읽어 consumer에 넘긴다. 메모리에는 항상 게임 한 판만 올라간다.
   * 다른 분석이 Lichess에서 게임을 받는 중이면 끝날 때까지 기다린다.
   */
  public void loadGames(String username, String perfType, String since, CancellationToken token,
      Consumer<LichessGameResponse> consumer) {
    try {
      // 기다리는 중에 취소되면 스레드 인터럽트로 바로 빠져나온다
      requestLock.lockInterruptibly();
    } catch (InterruptedException e) {
      throw cancelled(e);
    }
    try {
      for (int retry = 0; !token.isCancelled(); retry++) {
        try {
          stream(username, perfType, since, token, consumer);
          return;
        } catch (RateLimitedException e) {
          if (retry >= MAX_RATE_LIMIT_RETRIES) {
            throw e;
          }
          log.warn("Lichess rate limited. Waiting {} before retry {}/{}", rateLimitBackoff,
              retry + 1, MAX_RATE_LIMIT_RETRIES);
          // 잠금을 쥔 채 쉬므로 다른 분석의 Lichess 요청도 함께 멈춘다
          sleep(rateLimitBackoff);
        }
      }
    } finally {
      requestLock.unlock();
    }
  }

  private void stream(String username, String perfType, String since, CancellationToken token,
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
          if (response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
            throw new RateLimitedException("Lichess 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
          }
          if (response.getStatusCode().isError()) {
            throw new IllegalStateException("Lichess API error: " + response.getStatusCode());
          }

          InputStream body = response.getBody();
          token.onCancel(body);
          BufferedReader reader = new BufferedReader(
              new InputStreamReader(body, StandardCharsets.UTF_8));
          String line;
          while (!token.isCancelled() && (line = reader.readLine()) != null) {
            if (!line.isBlank()) {
              consumer.accept(objectMapper.readValue(line, LichessGameResponse.class));
            }
          }
          return null;
        });
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      throw cancelled(e);
    }
  }

  private static CancellationException cancelled(InterruptedException e) {
    // 인터럽트 상태를 되살려 둔다. JobHandle.fetch 가 끝날 때 지운다.
    Thread.currentThread().interrupt();
    CancellationException cancelled = new CancellationException("Lichess request cancelled");
    cancelled.initCause(e);
    return cancelled;
  }
}
