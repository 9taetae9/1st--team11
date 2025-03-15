package com.team11.hrbank.module.domain.employee.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import com.team11.hrbank.module.domain.employee.QEmployee;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRepositoryCustomImpl implements EmployeeRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  public EmployeeRepositoryCustomImpl(JPAQueryFactory jpaQueryFactory) {
    this.queryFactory = jpaQueryFactory;
  }

  // 직원 수 조회
  @Override
  public long countByStatusAndHireDateBetween(EmployeeStatus status, Instant fromData,
      Instant toDate) {

    QEmployee employee = QEmployee.employee;

    // 동적 쿼리 조건 구성
    BooleanBuilder builder = new BooleanBuilder();

    // status 조건 추가
    if (status != null) {
      builder.and(employee.status.eq(status));
    }

    //입사일 범위 조건 추가
    if (fromData != null || toDate != null) { // 둘 중 하나라도 있다면 입사일 조건 추가
      if (fromData != null) {
        builder.and(employee.hireDate.goe(fromData)); // 입사일이 fromDate 이후
      }
      if (toDate != null) {
        builder.and(employee.hireDate.loe(toDate)); // 입사일이 toDate 이전
      }
    }

    // 직원 수 카운트 쿼리
    Long count = queryFactory
        .select(employee.count())
        .from(employee)
        .where(builder)
        .fetchOne();

    return count != null ? count : 0L; // null일 경우 0 반환

  }
}
