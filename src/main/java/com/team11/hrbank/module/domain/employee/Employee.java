package com.team11.hrbank.module.domain.employee;

import com.team11.hrbank.module.domain.UpdatableEntity;
import com.team11.hrbank.module.domain.department.Department;
import com.team11.hrbank.module.domain.file.File;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Entity
@Table(name = "employees")
public class Employee extends UpdatableEntity {

  @NotNull
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @NotNull
  @Column(name = "email", nullable = false)
  private String email;

  @NotNull
  @Column(name = "employee_number", nullable = false, length = 25)
  private String employeeNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  private Department department;

  @NotNull
  @Column(name = "\"position\"", nullable = false, length = 50)
  private String position;

  @NotNull
  @Column(name = "hire_date", nullable = false)
  private Instant hireDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "profile_image_id")
  private File profileImage;

  @ColumnDefault("'ACTIVE'")
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private EmployeeStatus status;

  public Employee() {

  }

  @Builder
  public Employee(String name, String email, String employeeNumber, Department department,
      String position, Instant hireDate, File profileImage, EmployeeStatus status) {
    this.name = name;
    this.email = email;
    this.employeeNumber = employeeNumber;
    this.department = department;
    this.position = position;
    this.hireDate = hireDate;
    this.profileImage = profileImage;
    this.status = status;
  }

  // update 메서드 추가
  public void updateName(String name) {
    this.name = name;
  }

  public void updateEmail(String email) {
    this.email = email;
  }

  public void updateDepartment(Department department) {
    this.department = department;
  }

  public void updatePosition(String position) {
    this.position = position;
  }

  public void updateHireDate(Instant hireDate) {
    this.hireDate = hireDate;
  }

  public void updateProfileImage(File profileImage) {
    this.profileImage = profileImage;
  }

  public void updateStatus(EmployeeStatus status) {
    this.status = status;
  }
}
