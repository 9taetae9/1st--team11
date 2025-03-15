package com.team11.hrbank.module.domain.department.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentDto {
  private Long id; //api 명세에 지정되어있음
  private String name;
  private String description;
  private LocalDate establishDate; //api 명세서에 지정되어있음
  private Long employeeCount;

}
