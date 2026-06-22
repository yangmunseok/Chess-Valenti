package org.spring.createa.chessvalenti.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.dto.response.ChessComArchivesResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ChessComApiTest {

  private MockWebServer mockWebServer;
  private ChessComApi chessComApi;

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

    chessComApi = factory.createClient(ChessComApi.class);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void loadArchives_shouldCallCorrectUriAndParseJson() throws InterruptedException {
    // Given
    String jsonResponse = "{\"archives\":[\"https://api.chess.com/pub/player/testuser/games/2026/05\",\"https://api.chess.com/pub/player/testuser/games/2026/06\"]}";
    mockWebServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(jsonResponse));

    // When
    Mono<ChessComArchivesResponse> responseMono = chessComApi.loadArchives("testuser");

    // Then
    StepVerifier.create(responseMono)
        .expectNextMatches(response -> {
          List<String> list = response.archives();
          return list != null && list.size() == 2
              && list.get(0).endsWith("2026/05")
              && list.get(1).endsWith("2026/06");
        })
        .verifyComplete();

    // Verify request
    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", recordedRequest.getMethod());
    assertEquals("/player/testuser/games/archives/", recordedRequest.getPath());
  }

  @Test
  void loadMonthlyPgn_shouldCallCorrectUriAndReturnText() throws InterruptedException {
    // Given
    String pgnContent = "[Event \"Live Chess\"]\n[Site \"Chess.com\"]\n1. e4 e5 1-0\n";
    mockWebServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "text/plain")
        .setBody(pgnContent));

    // When
    Mono<String> responseMono = chessComApi.loadMonthlyPgn("testuser", "2026", "06");

    // Then
    StepVerifier.create(responseMono)
        .expectNext(pgnContent)
        .verifyComplete();

    // Verify request
    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", recordedRequest.getMethod());
    assertEquals("/player/testuser/games/2026/06/pgn", recordedRequest.getPath());
  }
}
