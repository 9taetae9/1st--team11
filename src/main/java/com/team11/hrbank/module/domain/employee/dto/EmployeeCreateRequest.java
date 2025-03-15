package com.team11.hrbank.module.domain.employee.dto;

import java.time.Instant;

public record EmployeeCreateRequest (
    String name,
    String email,
    Long departmentId,
    String position,
    Instant hireDate,
    String memo
){
}
