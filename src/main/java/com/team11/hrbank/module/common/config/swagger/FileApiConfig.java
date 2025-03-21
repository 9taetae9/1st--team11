package com.team11.hrbank.module.common.config.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileApiConfig {

    @Bean
    public GroupedOpenApi fileApi() {
        return GroupedOpenApi.builder()
                .group("파일 관리")
                .pathsToMatch("/api/files/**")
                .build();
    }
}
