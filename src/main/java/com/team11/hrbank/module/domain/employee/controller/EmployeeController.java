package com.team11.hrbank.module.domain.employee.controller;

import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import com.team11.hrbank.module.domain.employee.dto.CursorPageResponseEmployeeDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDistributionDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeUpdateRequest;
import com.team11.hrbank.module.domain.employee.service.EmployeeCommandService;
import com.team11.hrbank.module.domain.employee.service.EmployeeQueryService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

  /**
   * 추신: ErrorResponse 이용 여부
   * <p>
   * Swagger 스키마에 ErrorResponse가 이미 정의되어 있고 공통 요소인 것 같습니다. 그래서 ErrorResponse는 별도로 생성하지 않았습니다.
   * <p>
   * 에러 처리 규칙에 대해 생각해보는 것이 좋을 것 같습니다. 특히 줄곧 나오는 400 (Bad Request), 500 (Internal Server Error) 어떻게
   * 처리할지에 대한 규칙을 먼저 정한 후 적용하는 것이 좋을 것 같습니다.
   * <p>
   * 현재 상태에서는 직원 도메인 관련 API에서 발생하는 에러는 서비스 레벨에서 처리되고 있으며, 컨트롤러 단에서 발생할 수 있는 400, 500 에러에 대한 처리 로직은
   * 설정되지 않은 상태입니다. <
   * TODO:
   * 1. ErrorResponse 만들기
   * 2. 에러 처리 방식 통일하기
   */

  private final EmployeeCommandService employeeCommandService;
  private final EmployeeQueryService employeeQueryService;

  // TODO : 직원 생성

  // 직원 수정
  @PatchMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable Long id,
      @RequestPart(value = "employee") EmployeeUpdateRequest employeeUpdateRequest,
      @RequestPart(value = "profile", required = false) MultipartFile file) {
    return ResponseEntity.ok(
        employeeCommandService.updateEmployee(id, employeeUpdateRequest, file));
  }

  // 직원 삭제
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
    employeeCommandService.deleteEmployee(id);
    return ResponseEntity.noContent().build();
  }

  // 직원 상세 조회
  @GetMapping("/{id}")
  public ResponseEntity<EmployeeDto> getEmployeeDetails(@PathVariable Long id) {
    return ResponseEntity.ok(employeeQueryService.getEmployeeDetails(id));
  }

  /**
   * 추신 : 프로토 타입에는 사원 번호(부분 일치), 입사일(범위 조건)를 통한 "필터 작업"이 없습니다. 강사님께서 말한 API 의 불일치인 건 같습니다. 대신 사원 번호와
   * 입사일은 "정렬 작업"에 필요합니다.
   **/
  // 직원 목록 조회
  @GetMapping
  public ResponseEntity<CursorPageResponseEmployeeDto> getListEmployees(
      @RequestParam(required = false) String nameOrEmail,
      @RequestParam(required = false) String employeeNumber,
      @RequestParam(required = false) String departmentName,
      @RequestParam(required = false) String position,
      @RequestParam(required = false) Instant hireDateFrom,
      @RequestParam(required = false) Instant hireDateTo,
      @RequestParam(required = false) EmployeeStatus status,
      @RequestParam(required = false) Long idAfter,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "name") String sortField,
      @RequestParam(defaultValue = "asc") String sortDirection
  ) {

    return ResponseEntity.ok(employeeQueryService.getListEmployees(
        nameOrEmail,
        employeeNumber,
        departmentName,
        position,
        hireDateFrom,
        hireDateTo,
        status,
        idAfter,
        cursor,
        size,
        sortField,
        sortDirection));
  }

  // 직원 분포 조회
  @GetMapping("/stats/distribution")
  public ResponseEntity<List<EmployeeDistributionDto>> getEmployeeDistribution(
      @RequestParam(defaultValue = "department") String groupBy,
      @RequestParam(defaultValue = "ACTIVE") String status) {
    return ResponseEntity.ok(employeeQueryService.getEmployeeDistribution(groupBy, status));

  }

  // TODO : 직원 수 추이

  // 직원 수 조회
  @GetMapping("/count")
  public ResponseEntity<Long> getEmployeeCount(
      @RequestParam(required = false) EmployeeStatus status,
      @RequestParam(required = false) LocalDate fromDate,
      @RequestParam(required = false) LocalDate toDate) {

    return ResponseEntity.ok(employeeQueryService.getEmployeeCount(status,
        fromDate.atStartOfDay(ZoneId.of("UTC")).toInstant(),
        toDate.atStartOfDay(ZoneId.of("UTC")).toInstant()));
  }


}
