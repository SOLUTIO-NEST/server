package com.solutio.api.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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

        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .addServersItem(new Server().url("/"))
            .info(info)
            .components(
                new Components()
                    .addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .in(SecurityScheme.In.HEADER)
                    )
            );
    }
}
