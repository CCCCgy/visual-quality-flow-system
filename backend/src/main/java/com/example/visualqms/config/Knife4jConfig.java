package com.example.visualqms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI visualQmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Visual Quality Flow System API")
                        .description("Industrial visual inspection review and quality closed-loop system backend API")
                        .version("0.0.1"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .packagesToScan("com.example.visualqms.controller")
                .build();
    }
}
