package com.team11.hrbank.module.domain.department.service;

import com.team11.hrbank.module.domain.department.Department;
import com.team11.hrbank.module.domain.department.dto.DepartmentCreateRequest;
import com.team11.hrbank.module.domain.department.dto.DepartmentDto;
import com.team11.hrbank.module.domain.department.mapper.DepartmentMapper;
import com.team11.hrbank.module.domain.department.repository.DepartmentRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

  private DepartmentRepository departmentRepository;
  private DepartmentMapper departmentMapper;


  public DepartmentService(DepartmentRepository departmentRepository, DepartmentMapper departmentMapper/*, EmployeeRepository employeeRepository*/) {
    this.departmentRepository = departmentRepository;
    this.departmentMapper = departmentMapper;
//    this.employeeRepository = employeeRepository;

  }

  @Transactional
  public DepartmentDto createDepartment(DepartmentCreateRequest request) {
    //이름 중복 검사
    if (departmentRepository.existsByName(request.getName())) {
      throw new IllegalArgumentException("Department already exists" + request.getName());
    }

    Department department = departmentMapper.toDepartment(request);

    //BaseEntity에서 상속받은 createdAt 필드에 대한 setter 메서드에 대한 사항
    department.setCreatedAt(Instant.now());
    department.setUpdatedAt(Instant.now());

    Department savedDepartment = departmentRepository.save(department);

    return departmentMapper.toDepartmentDto(savedDepartment);
  }
}
