package com.team11.hrbank.module.domain.employee.repository;

import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import java.time.Instant;
import java.util.List;


public interface EmployeeRepositoryCustom {


  List<Employee> findEmployeesByConditions(
      String nameOrEmail,
      String employeeNumber,
      String departmentName,
      String position,
      Instant hireDateFrom,
      Instant hireDateTo,
      EmployeeStatus status,
      Long idAfter,
      String cursor,
      int size,
      String sortField,
      String sortDirection);

  long countByStatusAndHireDateBetween(
      EmployeeStatus status,
      Instant fromData,
      Instant toDate
  );

}
