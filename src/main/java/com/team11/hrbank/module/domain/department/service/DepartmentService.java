package com.team11.hrbank.module.domain.department.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team11.hrbank.module.domain.department.Department;
//import com.team11.hrbank.module.domain.employee.repository.EmployeeRepository;
import com.team11.hrbank.module.domain.department.dto.CursorPageResponseDepartmentDto;
import com.team11.hrbank.module.domain.department.dto.DepartmentCreateRequest;
import com.team11.hrbank.module.domain.department.dto.DepartmentDto;
import com.team11.hrbank.module.domain.department.dto.DepartmentUpdateRequest;
import com.team11.hrbank.module.domain.department.mapper.DepartmentMapper;
import com.team11.hrbank.module.domain.department.repository.DepartmentRepository;

public interface DepartmentService {

  // 부서 생성
  DepartmentDto createDepartment(DepartmentCreateRequest request);

  //부서 수정
  DepartmentDto updateDepartment(Long id, @Valid DepartmentUpdateRequest request);


  //부서 삭제
  void deleteDepartment(Long id);

  //개별 상세 조회
  DepartmentDto getDepartmentById(Long id);


  //부서 전체 조회 및 페이지네이션
  CursorPageResponseDepartmentDto getAllDepartments(
      String nameOrDescription,
      Long idAfter,
      String cursor,
      Integer size,
      String sortField,
      String sortDirection);
}