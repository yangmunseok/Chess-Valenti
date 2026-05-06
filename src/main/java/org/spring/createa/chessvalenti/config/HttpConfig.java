package org.spring.createa.chessvalenti.config;

import org.spring.createa.chessvalenti.service.LichessApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

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
}
