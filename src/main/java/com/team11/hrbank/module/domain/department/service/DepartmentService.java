package com.team11.hrbank.module.domain.department.service;

import com.team11.hrbank.module.domain.department.Department;
import com.team11.hrbank.module.domain.department.dto.DepartmentCreateRequest;
import com.team11.hrbank.module.domain.department.dto.DepartmentDto;
import com.team11.hrbank.module.domain.department.dto.DepartmentUpdateRequest;
import com.team11.hrbank.module.domain.department.mapper.DepartmentMapper;
import com.team11.hrbank.module.domain.department.repository.DepartmentRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.NoSuchElementException;
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

  @Transactional
  public DepartmentDto updateDepartment(Long id, @Valid DepartmentUpdateRequest request) {
    Department department = departmentRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Department not found: " + id));

    if (request.getName() != null && !request.getName().equals(department.getName())) {
      if (departmentRepository.existsByName(request.getName())) {
        throw new IllegalArgumentException("Department already exists with name: " + request.getName());
      }
    }

    department = departmentMapper.updateDepartmentFromRequest(department, request);

    department.setUpdatedAt(Instant.now());

    Department updatedDepartment = departmentRepository.save(department);
    return departmentMapper.toDepartmentDto(updatedDepartment);

  }

  public void deleteDepartment(Long id) {
    Department department = departmentRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Department not found: " + id));

    // 소속 직원이 있는지 확인
    // employee리포지토리에 작성되어있는 직원수 카운팅 메서드 확인후 변경 현재는 countByDepartmentId이라고 가정
//    long employeeCount = employeeRepository./*countByDepartmentId*/(id);
//    if (employeeCount > 0 ){
//      throw new IllegalArgumentException("Cannot delete department with associated employees");
//    }

    departmentRepository.delete(department);
  }

  public DepartmentDto getDepartmentById(Long id) {
    if (!departmentRepository.findById(id).isPresent()) {
      throw new NoSuchElementException("부서를 찾을 수 없습니다: " + id);
    }
    Department department = departmentRepository.findById(id).get();
    DepartmentDto departmentDto = departmentMapper.toDepartmentDto(department);
    // 나중에 구현: 직원수
    // departmentDto.setEmployeeCount(employeeRepository.countByDepartmentId(id));

    return departmentDto;
  }


}
