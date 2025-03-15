package com.team11.hrbank.module.domain.employee.controller;

import com.team11.hrbank.module.domain.employee.dto.EmployeeDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeUpdateRequest;
import com.team11.hrbank.module.domain.employee.service.EmployeeCommandService;
import com.team11.hrbank.module.domain.employee.service.EmployeeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

  private final EmployeeCommandService employeeCommandService;
  private final EmployeeQueryService employeeQueryService;

  // 직원 수정
  @PatchMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable Long id,
      @RequestPart(value = "employee") EmployeeUpdateRequest employeeUpdateRequest,
      @RequestPart(value = "profile") MultipartFile file) {
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

}
