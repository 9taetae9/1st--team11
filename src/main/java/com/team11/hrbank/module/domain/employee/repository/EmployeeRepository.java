package com.team11.hrbank.module.domain.employee.repository;

import com.team11.hrbank.module.domain.employee.Employee;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

  // 이메일 컬럼 접근
  @Query("SELECT e.email FROM Employee e")
  List<String> findAllEmails();

  // 기본 메서드를 이용하여 입사일과 상태로 직원 조회
  List<Employee> findByHireDateLessThanEqual(Instant toDate);

  int countEmployeesByHireDateBetween(Instant from, Instant to);


  long countByDepartmentId(Long departmentId);
}
