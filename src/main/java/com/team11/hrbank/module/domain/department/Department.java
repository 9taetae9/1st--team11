package com.team11.hrbank.module.domain.department;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import com.team11.hrbank.module.domain.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "departments")
public class Department extends BaseEntity {

  @NotNull
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "description", length = Integer.MAX_VALUE)
  private String description;

  @Column(name = "established_date", nullable = false)
  private Instant establishedDate;


  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}