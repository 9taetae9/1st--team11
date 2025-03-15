package com.team11.hrbank.module.domain.employee.mapper;

import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface EmployeeMapper {

  /* TODO 다른 사람들이 Mapper 사용하는 법 확인 필요 */
  @Mapping(source = "department.id", target = "departmentId")
  @Mapping(source = "department.name", target = "departmentName")
  @Mapping(source = "profileImage.id", target = "profileImageId")
  EmployeeDto toDto(Employee employee);

}
