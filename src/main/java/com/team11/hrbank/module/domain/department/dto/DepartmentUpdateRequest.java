package com.team11.hrbank.module.domain.department.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
public class DepartmentUpdateRequest {

  @Length(max = 100, message = "부서명은 100자를 초과할 수 없습니다.")
  private String name;

  private String description;

  private LocalDate establishedDate;
}