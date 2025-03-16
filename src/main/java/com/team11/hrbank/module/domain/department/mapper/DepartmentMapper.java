package com.team11.hrbank.module.domain.department.mapper;

import com.team11.hrbank.module.domain.department.Department;
import com.team11.hrbank.module.domain.department.dto.DepartmentCreateRequest;
import com.team11.hrbank.module.domain.department.dto.DepartmentDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {
  //  부서 생성 요청 DTO를 부서 엔티티로 변환
  public Department toDepartment(DepartmentCreateRequest departmentCreateRequest) {
    if (departmentCreateRequest == null) {
      return null;
    }

    Department department = new Department();
    department.setName(departmentCreateRequest.getName());
    department.setDescription(departmentCreateRequest.getDescription());

    // 설립일이 제공된 경우 LocalDate를 Instant로 변환하여 설정 -> api 상에서 로컬 데이터 활용하는 것 같음!
    if(departmentCreateRequest.getEstablishedDate() != null) {
      Instant establishedDate = departmentCreateRequest.getEstablishedDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
      department.setEstablishedDate(establishedDate);
    }
    return department;
  }

  // 부서 엔티티를 부서 DTO로 변환
  public DepartmentDto toDepartmentDto(Department department) {
    if (department == null) {
      return null;
    }

    DepartmentDto departmentDto = new DepartmentDto();
    departmentDto.setId(department.getId());
    departmentDto.setName(department.getName());
    departmentDto.setDescription(department.getDescription());

    // 설립일이 존재하는 경우 Instant를 LocalDate로 변환하여 설정 = 위와 같은 이유
    if(department.getEstablishedDate() != null) {
      LocalDate establishedDate = LocalDate.ofInstant(department.getEstablishedDate(), ZoneId.systemDefault());
      departmentDto.setEstablishedDate(establishedDate);
    }

    // 직원 수 초기값을 0으로 설정
    departmentDto.setEmployeeCount(0L);

    return departmentDto;
  }


}
