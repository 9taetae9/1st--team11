package com.team11.hrbank.module.common.config.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployeeApiConfig {

    @Bean
    public GroupedOpenApi employeeApi() {
        return GroupedOpenApi.builder()
                .group("직원 관리") // Swagger UI에서 표시될 그룹 이름
                .pathsToMatch("/api/employees/**") // 직원 관련 API 그룹화
                .build();
    }
}
