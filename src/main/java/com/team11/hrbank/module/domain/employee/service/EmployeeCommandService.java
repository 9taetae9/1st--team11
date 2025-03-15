package com.team11.hrbank.module.domain.employee.service;

import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeUpdateRequest;
import com.team11.hrbank.module.domain.employee.mapper.EmployeeMapper;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EmployeeCommandService {

  private final EmployeeRepository employeeRepository;
  private final EmployeeMapper employeeMapper;

  // 직원 수정
  @Transactional
  public EmployeeDto updateEmployee(Long id, EmployeeUpdateRequest employeeUpdateRequest,
      MultipartFile file) {

    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("employee(" + id + ")는 존재하지 않습니다."));

    if (employeeUpdateRequest.name() != null) {
      employee.updateName(employeeUpdateRequest.name());
    }
    if (employeeUpdateRequest.email() != null) {
      /** 요구 조건 : 이메일 중복 여부 검증, 중복으로 검증 로직 분리 고려 (된다면)**/
      if (
          employeeRepository.findAllEmails().stream()
              .anyMatch(email -> email.equals(employeeUpdateRequest.email()))
      ) {
        throw new IllegalArgumentException(
            "email(" + employeeUpdateRequest.email() + ")은 이미 존재합니다.");
      }
      employee.updateEmail(employeeUpdateRequest.email());
    }
    if (employeeUpdateRequest.departmentId() != null) {
      /* 이 부분 TODO (#Department) 부서 id를 통해 부서 객체 전달받기 -- 지현씨께 */
    }
    if (employeeUpdateRequest.position() != null) {
      employee.updatePosition(employeeUpdateRequest.position());
    }
    if (employeeUpdateRequest.hireDate() != null) {
      employee.updateHireDate(employeeUpdateRequest.hireDate());
    }
    if (employeeUpdateRequest.status() != null) {
      employee.updateStatus(employeeUpdateRequest.status());
    }
    if (employeeUpdateRequest.memo() != null) {
      /* 이 부분 TODO (#Change-log) memo 데이터를 Change-log 로 전달 -- 태현씨께 */
    }
    if (file != null) {
      /* 이 부분 TODO (#File) 프로필 이미지 처리 -- byte 데이터 처리 및 메타데이터를 넘겨주는 자리 -- 건희씨께 */
    }

    return employeeMapper.toDto(employee);
  }

}
