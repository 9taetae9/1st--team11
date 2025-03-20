package com.team11.hrbank.module.common.config.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DepartmentApiConfig {

    @Bean
    public GroupedOpenApi departmentApi() {
        return GroupedOpenApi.builder()
                .group("부서 관리") // Swagger UI에서 표시될 그룹 이름
                .pathsToMatch("/api/departments/**") // 이 경로에 해당하는 API를 그룹화
                .build();
    }
}
