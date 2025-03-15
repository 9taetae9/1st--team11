package com.team11.hrbank.module.domain.employee.service;

import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDto;
import com.team11.hrbank.module.domain.employee.mapper.EmployeeMapper;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepository;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepositoryCustom;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeQueryService {

  private final EmployeeRepository employeeRepository;
  private final EmployeeRepositoryCustom employeeRepositoryCustom;
  private final EmployeeMapper employeeMapper;

  // 직원 상세 조회
  public EmployeeDto getEmployeeDetails(Long id) {
    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("employee(" + id + ")는 존재하지 않습니다."));
    return employeeMapper.toDto(employee);
  }

  // 직원 수 조회
  public long getEmployeeCount(EmployeeStatus status, Instant fromDate, Instant toDate) {
    return employeeRepositoryCustom.countByStatusAndHireDateBetween(status, fromDate, toDate);
  }

}
