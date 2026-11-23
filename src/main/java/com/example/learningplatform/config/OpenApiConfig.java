package com.example.learningplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI learningPlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Learning Platform API")
                        .description("REST API учебной платформы")
                        .version("v1"));
    }
}
