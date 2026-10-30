package org.spring.createa.chessvalenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.config.HttpConfig;
import org.spring.createa.chessvalenti.dto.response.LichessGameResponse;
import org.spring.createa.chessvalenti.exception.RateLimitedException;
import org.spring.createa.chessvalenti.exception.UserNotFoundException;
import org.springframework.web.client.RestClient;

public class LichessApiTest {

  private static final String GAME_LINE = "{\"winner\":\"white\",\"pgn\":\"1. e4 e5\",\"moves\":\"e2e4 e7e5\",\"players\":{\"white\":{\"user\":{\"name\":\"user1\",\"id\":\"user1\"}},\"black\":{\"user\":{\"name\":\"user2\",\"id\":\"user2\"}}},\"variant\":\"standard\",\"unknownField\":1}\n";

  private MockWebServer mockWebServer;
  private LichessApi lichessApi;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    RestClient restClient = RestClient.builder()
        .baseUrl(mockWebServer.url("/").toString())
        .requestFactory(HttpConfig.requestFactory())
        .build();
    ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // 429 후 1분씩 기다리면 테스트가 느려지므로 짧게 줄인다
    lichessApi = new LichessApi(restClient, objectMapper, Duration.ofMillis(100));
  }

  @AfterEach
  void tearDown() {
    try {
      mockWebServer.shutdown();
    } catch (IOException ignored) {
      // 느린 응답 테스트에서는 서버 스레드가 아직 전송 대기 중이라 종료 대기가 시간 초과될 수 있음
    }
  }

  @Test
  void loadGames_shouldCallCorrectUriAndParseNdjson() throws InterruptedException {
    String secondGame = "{\"winner\":\"black\",\"pgn\":\"1. d4 d5\",\"moves\":\"d2d4 d7d5\",\"players\":{\"white\":{\"user\":{\"name\":\"user2\",\"id\":\"user2\"}},\"black\":{\"user\":{\"name\":\"user1\",\"id\":\"user1\"}}},\"variant\":\"standard\"}\n";
    mockWebServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/x-ndjson")
        .setBody(GAME_LINE + "\n" + secondGame));
    List<LichessGameResponse> games = new CopyOnWriteArrayList<>();

    lichessApi.loadGames("user1", "blitz", "123456", new JobHandle(), games::add);

    assertThat(games).hasSize(2);
    assertThat(games.get(0).winner()).isEqualTo("white");
    assertThat(games.get(0).players().white().user().name()).isEqualTo("user1");
    assertThat(games.get(1).winner()).isEqualTo("black");
    assertThat(games.get(1).players().white().user().name()).isEqualTo("user2");

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", recordedRequest.getMethod());
    assertEquals("/games/user/user1?pgnInJson=true&perfType=blitz&since=123456",
        recordedRequest.getPath());
    assertEquals("application/x-ndjson", recordedRequest.getHeader("Accept"));
  }

  @Test
  void loadGames_whenUserDoesNotExist_shouldThrowUserNotFound() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(404));

    assertThatThrownBy(() -> lichessApi.loadGames("nobody", "blitz", "123456", new JobHandle(),
        game -> {
        })).isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void loadGames_whenCancelledWhileWaiting_shouldStopImmediately() {
    // 첫 게임만 보내고 다음 게임은 30초 뒤에야 보내는 느린 서버
    mockWebServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/x-ndjson")
        .setBody(GAME_LINE.repeat(3))
        .throttleBody(GAME_LINE.length(), 30, TimeUnit.SECONDS));
    JobHandle handle = new JobHandle();
    List<LichessGameResponse> games = new CopyOnWriteArrayList<>();

    Thread canceller = new Thread(() -> {
      try {
        Thread.sleep(500);
      } catch (InterruptedException ignored) {
        return;
      }
      handle.cancel();
    });

    long start = System.nanoTime();
    canceller.start();
    try {
      handle.fetch(() -> lichessApi.loadGames("user1", "blitz", "123456", handle, games::add));
    } catch (RuntimeException expected) {
      // 스트림이 닫혀서 읽기가 예외로 끝나는 것이 정상
    }
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    assertThat(games).hasSize(1);
    assertThat(elapsedMillis).isLessThan(5_000);
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  private static MockResponse slowGames() {
    // 게임 3판을 약 1.2초에 걸쳐 보내는 응답
    return new MockResponse()
        .setHeader("Content-Type", "application/x-ndjson")
        // 응답 후 연결을 닫아 테스트 종료 때 서버가 keep-alive 연결을 기다리지 않게 한다
        .setHeader("Connection", "close")
        .setBody(GAME_LINE.repeat(3))
        .throttleBody(GAME_LINE.length(), 400, TimeUnit.MILLISECONDS);
  }

  private static MockResponse games() {
    return new MockResponse()
        .setHeader("Content-Type", "application/x-ndjson")
        .setHeader("Connection", "close")
        .setBody(GAME_LINE);
  }

  @Test
  void loadGames_whenRateLimited_shouldWaitAndRetry() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(429));
    mockWebServer.enqueue(games());
    List<LichessGameResponse> loaded = new CopyOnWriteArrayList<>();

    lichessApi.loadGames("user1", "blitz", "123456", new JobHandle(), loaded::add);

    assertThat(loaded).hasSize(1);
    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }

  @Test
  void loadGames_whenRateLimitDoesNotClear_shouldGiveUp() {
    for (int i = 0; i < 3; i++) {
      mockWebServer.enqueue(new MockResponse().setResponseCode(429));
    }

    assertThatThrownBy(() -> lichessApi.loadGames("user1", "blitz", "123456", new JobHandle(),
        game -> {
        })).isInstanceOf(RateLimitedException.class);
    assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
  }

  @Test
  void loadGames_shouldSendOneRequestAtATime() throws InterruptedException {
    mockWebServer.enqueue(slowGames());
    mockWebServer.enqueue(games());
    List<LichessGameResponse> first = new CopyOnWriteArrayList<>();
    List<LichessGameResponse> second = new CopyOnWriteArrayList<>();

    Thread a = new Thread(() ->
        lichessApi.loadGames("user1", "blitz", "123456", new JobHandle(), first::add));
    a.start();
    Thread.sleep(200);
    Thread b = new Thread(() ->
        lichessApi.loadGames("user2", "blitz", "123456", new JobHandle(), second::add));
    b.start();

    Thread.sleep(400);
    // 첫 요청이 아직 받는 중이므로 두 번째 요청은 서버에 도착하지 않았어야 한다
    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);

    a.join(5_000);
    b.join(5_000);
    assertThat(first).hasSize(3);
    assertThat(second).hasSize(1);
    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }

  @Test
  void loadGames_whenCancelledWhileWaitingForTurn_shouldStopWithoutRequest()
      throws InterruptedException {
    mockWebServer.enqueue(slowGames());
    Thread a = new Thread(() ->
        lichessApi.loadGames("user1", "blitz", "123456", new JobHandle(), game -> {
        }));
    a.start();
    Thread.sleep(200);

    JobHandle waiting = new JobHandle();
    AtomicReference<RuntimeException> error = new AtomicReference<>();
    Thread b = new Thread(() -> {
      try {
        waiting.fetch(() -> lichessApi.loadGames("user2", "blitz", "123456", waiting, game -> {
        }));
      } catch (RuntimeException e) {
        error.set(e);
      }
    });
    b.start();
    Thread.sleep(200);

    waiting.cancel();
    b.join(1_000);

    assertThat(b.isAlive()).isFalse();
    assertThat(error.get()).isInstanceOf(CancellationException.class);
    a.join(5_000);
    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
  }
}
