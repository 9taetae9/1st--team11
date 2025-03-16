package com.team11.hrbank.module.domain.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
public class DepartmentCreateRequest {

  @NotBlank(message = "부서명을 입력해주세요.") //빈칸 입력 불가능함
  @Length(max = 100, message = "부서명은 100자를 초과할 수 없습니다.")
  private String name;

  @NotNull(message = "설립일을 입력해주세요.")//빈칸 입력 불가능함
  private LocalDate establishedDate;

  @NotBlank(message = "부서 설명을 입력해주세요.")//빈칸 입력 불가능함
  private String description;
}
