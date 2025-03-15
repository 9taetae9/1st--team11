package com.team11.hrbank.module.domain.employee.repository;

import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import java.time.Instant;


public interface EmployeeRepositoryCustom {

  long countByStatusAndHireDateBetween(
      EmployeeStatus status,
      Instant fromData,
      Instant toDate
  );

}
