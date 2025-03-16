package com.team11.hrbank.module.domain.department.repository;

import com.team11.hrbank.module.domain.department.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
  boolean existsByName(String name);

}
