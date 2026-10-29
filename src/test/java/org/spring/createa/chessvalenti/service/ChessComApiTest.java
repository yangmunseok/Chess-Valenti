package org.spring.createa.chessvalenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.createa.chessvalenti.config.HttpConfig;
import org.spring.createa.chessvalenti.dto.response.ChessComArchivesResponse;
import org.spring.createa.chessvalenti.exception.UserNotFoundException;
import org.springframework.web.client.RestClient;

public class ChessComApiTest {

  private MockWebServer mockWebServer;
  private ChessComApi chessComApi;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    RestClient restClient = RestClient.builder()
        .baseUrl(mockWebServer.url("/").toString())
        .requestFactory(HttpConfig.requestFactory())
        .build();

    chessComApi = new ChessComApi(restClient);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void loadArchives_shouldCallCorrectUriAndParseJson() throws InterruptedException {
    String jsonResponse = "{\"archives\":[\"https://api.chess.com/pub/player/testuser/games/2026/05\",\"https://api.chess.com/pub/player/testuser/games/2026/06\"]}";
    mockWebServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(jsonResponse));

    ChessComArchivesResponse response = chessComApi.loadArchives("testuser");

    assertThat(response.archives()).hasSize(2);
    assertThat(response.archives().get(0)).endsWith("2026/05");
    assertThat(response.archives().get(1)).endsWith("2026/06");

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", recordedRequest.getMethod());
    assertEquals("/player/testuser/games/archives/", recordedRequest.getPath());
  }

  @Test
  void loadArchives_whenUserDoesNotExist_shouldThrowUserNotFound() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(404));

    assertThatThrownBy(() -> chessComApi.loadArchives("nobody"))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void loadMonthlyPgn_shouldSplitIntoGames() throws InterruptedException {
    String pgnContent = "[Event \"Live Chess\"]\n[Site \"Chess.com\"]\n\n1. e4 e5 1-0\n\n"
        + "[Event \"Live Chess\"]\n[Site \"Chess.com\"]\n\n1. d4 d5 0-1\n";
    mockWebServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "text/plain")
        .setBody(pgnContent));
    List<String> games = new ArrayList<>();

    chessComApi.loadMonthlyPgn("testuser", "2026", "06", new JobHandle(), games::add);

    assertThat(games).containsExactly(
        "[Event \"Live Chess\"]\n[Site \"Chess.com\"]\n\n1. e4 e5 1-0",
        "[Event \"Live Chess\"]\n[Site \"Chess.com\"]\n\n1. d4 d5 0-1");

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", recordedRequest.getMethod());
    assertEquals("/player/testuser/games/2026/06/pgn", recordedRequest.getPath());
  }
}
