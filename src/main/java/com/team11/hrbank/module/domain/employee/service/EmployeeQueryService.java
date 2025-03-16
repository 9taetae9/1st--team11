package com.team11.hrbank.module.domain.employee.service;

import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import com.team11.hrbank.module.domain.employee.dto.CursorPageResponseEmployeeDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDistributionDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDto;
import com.team11.hrbank.module.domain.employee.mapper.EmployeeMapper;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepository;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepositoryCustom;
import java.time.Instant;
import java.util.List;
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

  // 직원 목록 조회
  public CursorPageResponseEmployeeDto getListEmployees(
      String nameOrEmail,
      String employeeNumber,
      String departmentName,
      String position,
      Instant hireDateFrom,
      Instant hireDateTo,
      EmployeeStatus status,
      Long idAfter,
      String cursor,
      Integer size,
      String sortField,
      String sortDirection
  ) {
    List<Employee> employees = employeeRepositoryCustom.findEmployeesByConditions(
        nameOrEmail,
        employeeNumber,
        departmentName,
        position,
        hireDateFrom,
        hireDateTo,
        status,
        idAfter,
        cursor,
        size + 1, // todo 이거 좀 더 파악
        sortField,
        sortDirection);

    String nextCursor = null;
    Long nextIdAfter;
    boolean hasNext = false;

    if (employees.size() > size) {
      // size + 1로 조회했기 때문에, 마지막 직원은 실제 목록에 포함되지 않음
      employees.remove(employees.size() - 1);  // 마지막 직원 제거
      hasNext = true;
    }

    Employee lastEmployee = employees.get(employees.size() - 1);

    switch (cursor) {
      case "name":
        nextCursor = lastEmployee.getName();
        break;
      case "employeeNumber":
        nextCursor = lastEmployee.getEmployeeNumber();
        break;
      case "hireDate":
        nextCursor = lastEmployee.getHireDate().toString();
        break;
    }

    nextIdAfter = lastEmployee.getId();

    return new CursorPageResponseEmployeeDto(
        employees.stream()
            .map(employeeMapper::toDto).toList(),
        nextCursor,
        nextIdAfter,
        employees.size(),
        getEmployeeCount(status, hireDateFrom, hireDateTo),
        hasNext
    );
  }

  // 직원 분포 조회
  public List<EmployeeDistributionDto> getEmployeeDistribution(String groupBy, String status) {
    return employeeRepositoryCustom.findEmployeeDistribution(groupBy,
        EmployeeStatus.valueOf(status));
  }

  // 직원 수 조회
  public long getEmployeeCount(EmployeeStatus status, Instant fromDate, Instant toDate) {
    return employeeRepositoryCustom.countByStatusAndHireDateBetween(status, fromDate, toDate);
  }

}
