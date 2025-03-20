package com.team11.hrbank.module.common.config.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChangeLogApiConfig {

    @Bean
    public GroupedOpenApi changeLogApi() {
        return GroupedOpenApi.builder()
                .group("직원 정보 수정 이력 관리")
                .pathsToMatch("/api/change-logs/**")
                .build();
    }
}
