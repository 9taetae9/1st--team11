package com.team11.hrbank.module.domain.employee.repository;

import com.team11.hrbank.module.domain.employee.Employee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

  // 이메일 컬럼 접근
  @Query("SELECT e.email FROM Employee e")
  List<String> findAllEmails();

  long countByDepartmentId(Long departmentId);
}
