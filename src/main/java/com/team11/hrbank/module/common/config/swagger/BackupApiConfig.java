package com.team11.hrbank.module.common.config.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackupApiConfig {

    @Bean
    public GroupedOpenApi backupApi() {
        return GroupedOpenApi.builder()
                .group("데이터 백업 관리") // Swagger UI에서 표시될 그룹 이름
                .pathsToMatch("/api/backups/**") // 백업 관련 API 그룹화
                .build();
    }
}
