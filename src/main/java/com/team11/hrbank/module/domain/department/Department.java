package com.team11.hrbank.module.domain.department;

import com.team11.hrbank.module.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

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

  /**
   * BaseEntity에서 상속받은 createdAt 필드에 대한 setter 메서드.
   *
   * 현재 애플리케이션에 @EnableJpaAuditing 설정이 없어 @CreatedDate 어노테이션이 작동하지 않습니다.
   *
   * 이 메서드가 없으면 다음 오류가 발생합니다:
   * "ERROR: null value in column "created_at" of relation "departments" violates not-null constraint"
   * -> departments 테이블의 created_at 컬럼은 NOT NULL 설정이 되어 있음. 하지만 INSERT할 때 created_at 값이 NULL로 들어가서 오류 발생.
   *
   * 해결방안
   * 1. 지금과 같이 개인 엔터티와 서비스 수정한다.
   * 2. HrBankApplication에 @EnableJpaAuditing을 추가한다.
   */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}