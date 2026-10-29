package org.spring.createa.chessvalenti.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.spring.createa.chessvalenti.service.ChessComApi;
import org.spring.createa.chessvalenti.service.LichessApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class HttpConfig {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  // 응답 헤더가 올 때까지의 상한. 본문 스트리밍 중의 정체는 JobService의 타임아웃이 처리한다.
  private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

  @Bean
  public LichessApi lichessApi(ObjectMapper objectMapper) {
    RestClient restClient = RestClient.builder()
        .baseUrl("https://lichess.org/api/")
        .requestFactory(requestFactory())
        .build();

    return new LichessApi(restClient, objectMapper);
  }

  @Bean
  public ChessComApi chessComApi() {
    RestClient restClient = RestClient.builder()
        .baseUrl("https://api.chess.com/pub/")
        .defaultHeader("User-Agent", "chess-valenti insight loader")
        .requestFactory(requestFactory())
        .build();

    return new ChessComApi(restClient);
  }

  // JDK HttpClient는 블로킹된 읽기가 스레드 인터럽트로 깨어나므로 취소가 즉시 반영된다.
  public static JdkClientHttpRequestFactory requestFactory() {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(RESPONSE_TIMEOUT);
    return factory;
  }
}
