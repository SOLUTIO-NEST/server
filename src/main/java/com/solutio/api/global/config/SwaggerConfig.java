package com.solutio.api.global.config;

import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI springBootAPI() {

        Info info = new Info()
            .title("Solutio Nest Rest API Documentation")
            .description("Solutio 홈페이지인 Nest 서버의 API 문서입니다.")
            .version("0.1");

        return new OpenAPI()
            .addServersItem(new Server().url("/"))
            .info(info);
    }
}
