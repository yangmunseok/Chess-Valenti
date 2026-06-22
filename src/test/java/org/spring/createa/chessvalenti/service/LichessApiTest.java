package org.spring.createa.chessvalenti.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.dto.response.LichessGameResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class LichessApiTest {

  private MockWebServer mockWebServer;
  private LichessApi lichessApi;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    WebClient webClient = WebClient.builder()
        .baseUrl(mockWebServer.url("/").toString())
        .build();

    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(WebClientAdapter.create(webClient))
        .build();

    lichessApi = factory.createClient(LichessApi.class);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void loadGames_shouldCallCorrectUriAndParseNdjson() throws InterruptedException {
    // Given
    String ndjsonResponse = "{\"winner\":\"white\",\"pgn\":\"1. e4 e5\",\"moves\":\"e2e4 e7e5\",\"players\":{\"white\":{\"user\":{\"name\":\"user1\",\"id\":\"user1\"}},\"black\":{\"user\":{\"name\":\"user2\",\"id\":\"user2\"}}},\"variant\":\"standard\"}\n"
        + "{\"winner\":\"black\",\"pgn\":\"1. d4 d5\",\"moves\":\"d2d4 d7d5\",\"players\":{\"white\":{\"user\":{\"name\":\"user2\",\"id\":\"user2\"}},\"black\":{\"user\":{\"name\":\"user1\",\"id\":\"user1\"}}},\"variant\":\"standard\"}\n";

    mockWebServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/x-ndjson")
        .setBody(ndjsonResponse));

    // When
    Flux<LichessGameResponse> responseFlux = lichessApi.loadGames("user1", true, "blitz", "123456");

    // Then
    StepVerifier.create(responseFlux)
        .expectNextMatches(game -> "white".equals(game.winner())
            && "user1".equals(game.players().white().user().name())
            && "standard".equals(game.variant()))
        .expectNextMatches(game -> "black".equals(game.winner())
            && "user2".equals(game.players().white().user().name())
            && "standard".equals(game.variant()))
        .verifyComplete();

    // Verify request
    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", recordedRequest.getMethod());
    String path = recordedRequest.getPath();
    assertNotNull(path);
    // Path should match games/user/user1 with query parameters
    assertEquals("/games/user/user1?pgnInJson=true&perfType=blitz&since=123456", path);
    assertEquals("application/x-ndjson", recordedRequest.getHeader("Accept"));
  }
}
