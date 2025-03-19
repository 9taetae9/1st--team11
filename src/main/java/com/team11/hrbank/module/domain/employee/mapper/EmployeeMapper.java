package com.team11.hrbank.module.domain.employee.mapper;

import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

  @Mapping(source = "department.id", target = "departmentId")
  @Mapping(source = "department.name", target = "departmentName")
  @Mapping(source = "profileImage.id", target = "profileImageId")
  @Mapping(source = "hireDate", target = "hireDate", qualifiedByName = "instantToLocalDate")
  EmployeeDto toDto(Employee employee);

  @Named("instantToLocalDate")
  default LocalDate instantTolLocalDate(Instant instant) {
    return instant.atZone(ZoneId.of("UTC")).toLocalDate();
  }

}
