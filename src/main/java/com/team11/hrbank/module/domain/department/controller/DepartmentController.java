package com.team11.hrbank.module.domain.department.controller;

import com.team11.hrbank.module.domain.department.dto.DepartmentCreateRequest;
import com.team11.hrbank.module.domain.department.dto.DepartmentDto;
import com.team11.hrbank.module.domain.department.dto.DepartmentUpdateRequest;
import com.team11.hrbank.module.domain.department.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

  private final DepartmentService departmentService;

  public DepartmentController(DepartmentService departmentService) {
    this.departmentService = departmentService;
  }

  @PostMapping
  public ResponseEntity<DepartmentDto> createDepartment(@RequestBody @Valid DepartmentCreateRequest request) {
    DepartmentDto department = departmentService.createDepartment(request);
    return ResponseEntity.ok(department);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable("id") Long id, @RequestBody @Valid DepartmentUpdateRequest request) {
    DepartmentDto updateDepartment = departmentService.updateDepartment(id, request);
    return ResponseEntity.ok(updateDepartment);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteDepartment(@PathVariable Long id) {
    departmentService.deleteDepartment(id);
    return ResponseEntity.ok("부서 ID: " + id + " 삭제되었습니다");
  }

}
