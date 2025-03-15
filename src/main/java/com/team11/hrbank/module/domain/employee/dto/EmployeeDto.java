package com.team11.hrbank.module.domain.employee.dto;

import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import java.time.Instant;

public record EmployeeDto (
    Long id,
    String name,
    String email,
    String employeeNumber,
    Long departmentId,
    String departmentName,
    String position,
    Instant hireDate,
    EmployeeStatus status,
    Long profileImageId
){
}
