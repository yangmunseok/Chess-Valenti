package org.spring.createa.chessvalenti.config;

import org.spring.createa.chessvalenti.service.LichessApi;
import org.spring.createa.chessvalenti.service.ChessComApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;

@Configuration(proxyBeanMethods = false)
public class HttpConfig {

  @Bean
  public LichessApi lichessApi() {
    WebClient webClient = WebClient.builder()
        .baseUrl("https://lichess.org/api/")
        .build();
    
    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(WebClientAdapter.create(webClient))
        .build();
    
    return factory.createClient(LichessApi.class);
  }

  @Bean
  public ChessComApi chessComApi() {
    WebClient webClient = WebClient.builder()
        .baseUrl("https://api.chess.com/pub/")
        .defaultHeader("User-Agent", "chess-valenti insight loader")
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        .build();

    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(WebClientAdapter.create(webClient))
        .build();

    return factory.createClient(ChessComApi.class);
  }
}
