package org.spring.createa.chessvalenti.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chess Valenti API Documentation")
                        .description("Chess Valenti 서비스의 REST API 명세서입니다.")
                        .version("v0.0.1"));
    }
}
