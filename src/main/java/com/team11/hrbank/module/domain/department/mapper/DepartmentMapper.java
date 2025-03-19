package com.team11.hrbank.module.domain.department.mapper;

import com.team11.hrbank.module.domain.department.Department;
import com.team11.hrbank.module.domain.department.dto.DepartmentCreateRequest;
import com.team11.hrbank.module.domain.department.dto.DepartmentDto;
import com.team11.hrbank.module.domain.department.dto.DepartmentUpdateRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;


@Component
public class DepartmentMapper {

  // 부서 생성 요청 DTO -> 부서 엔티티 변환
  public Department toDepartment(DepartmentCreateRequest departmentCreateRequest) {
    if (departmentCreateRequest == null) {
      return null;
    }

    Department department = new Department();
    department.setName(departmentCreateRequest.getName());
    department.setDescription(departmentCreateRequest.getDescription());

    // LocalDate → Instant 변환 (UTC 기준)
    if (departmentCreateRequest.getEstablishedDate() != null) {
      department.setEstablishedDate(
          departmentCreateRequest.getEstablishedDate().atStartOfDay().toInstant(ZoneOffset.UTC)
      );
    }

    return department;
  }

  // 부서 엔티티 -> 부서 DTO 변환
  public DepartmentDto toDepartmentDto(Department department) {
    if (department == null) {
      return null;
    }

    DepartmentDto departmentDto = new DepartmentDto();
    departmentDto.setId(department.getId());
    departmentDto.setName(department.getName());
    departmentDto.setDescription(department.getDescription());

    // Instant → LocalDate 변환 (UTC 기준)
    if (department.getEstablishedDate() != null) {
      departmentDto.setEstablishedDate(
          department.getEstablishedDate().atZone(ZoneOffset.UTC).toLocalDate()
      );
    }

    // 직원 수 초기값을 0으로 설정
    departmentDto.setEmployeeCount(0L);

    return departmentDto;
  }

  // 부서 업데이트 요청 DTO -> 기존 부서 엔티티 업데이트
  public Department updateDepartmentFromRequest(Department department, @Valid DepartmentUpdateRequest updateRequest) {
    if (updateRequest == null || department == null) {
      return department;
    }

    if (updateRequest.getName() != null) {
      department.setName(updateRequest.getName());
    }

    if (updateRequest.getDescription() != null) {
      department.setDescription(updateRequest.getDescription());
    }

    // LocalDate → Instant 변환 (UTC 기준)
    if (updateRequest.getEstablishedDate() != null) {
      department.setEstablishedDate(
          updateRequest.getEstablishedDate().atStartOfDay().toInstant(ZoneOffset.UTC)
      );
    }

    return department;
  }
}
