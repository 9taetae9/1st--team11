package com.team11.hrbank.module.domain.department.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursorPageResponseDepartmentDto {
  private List<DepartmentDto> content;       // 페이지 내용(부서 목록)
  private String nextCursor;                // 다음 페이지 커서 추가
  private Long nextIdAfter;                 // 다음 페이지 시작 ID
  private Integer size;                     // 페이지 크기 Api 기반으로 integer
  private Long totalElements;               // 전체 요소 수
  private Boolean hasNext;                  // 다음 페이지 존재 여부
}