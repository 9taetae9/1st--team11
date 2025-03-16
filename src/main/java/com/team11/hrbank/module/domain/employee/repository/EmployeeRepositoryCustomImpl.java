package com.team11.hrbank.module.domain.employee.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import com.team11.hrbank.module.domain.employee.QEmployee;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDistributionDto;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRepositoryCustomImpl implements EmployeeRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  public EmployeeRepositoryCustomImpl(JPAQueryFactory jpaQueryFactory) {
    this.queryFactory = jpaQueryFactory;
  }

  // 직원 목록 조회
  @Override
  public List<Employee> findEmployeesByConditions(String nameOrEmail, String employeeNumber,
      String departmentName, String position, Instant hireDateFrom, Instant hireDateTo,
      EmployeeStatus status, Long idAfter, String cursor, int size, String sortField,
      String sortDirection) {
    BooleanBuilder builder = new BooleanBuilder();
    QEmployee employee = QEmployee.employee;

    // 조회 조건 적용
    if (nameOrEmail != null) {
      builder.and(employee.name.contains(nameOrEmail))
          .or(employee.email.contains(nameOrEmail));
    }
    if (employeeNumber != null) {
      builder.and(employee.employeeNumber.eq(employeeNumber));
    }
    if (departmentName != null) {
      builder.and(employee.department.name.contains(departmentName));
    }
    if (position != null) {
      builder.and(employee.position.contains(position));
    }
    if (hireDateFrom != null || hireDateTo != null) {
      if (hireDateFrom != null) {
        builder.and(employee.hireDate.goe(hireDateFrom));
      }
      if (hireDateTo != null) {
        builder.and(employee.hireDate.loe(hireDateTo));
      }
    }
    if (status != null) {
      builder.and(employee.status.eq(status));
    }

    // 커서 기반 페이지네이션
    if (cursor != null && idAfter != null) {
      builder.and(buildCursorCondition(idAfter, cursor, sortField, sortDirection, employee));
    } else if (cursor == null && idAfter == null) {
      // 첫 페이지인 경우에는 커서나 idAfter 없이 데이터를 조회 TODO 다시 체크
      builder.and(employee.id.gt(0L));
    } else if (idAfter != null) { // 이런 경우가 존재할지는 모르겠는데, 있을 수 있따고 하여 첨부
      builder.and(employee.id.gt(idAfter));
    }

    // 정렬
    OrderSpecifier<?> orderSpecifier = createOrderSpecifier(sortField, sortDirection, employee);

    return queryFactory
        .selectFrom(employee)
        .where(builder)
        .orderBy(orderSpecifier)
        .limit(size)
        .fetch();
  }

  @Override
  public List<EmployeeDistributionDto> findEmployeeDistribution(String groupBy,
      EmployeeStatus status) {
    QEmployee employee = QEmployee.employee;

    // 조건에 따른 필터링
    BooleanBuilder builder = new BooleanBuilder();
    if (status != null) {
      builder.and(employee.status.eq(status));
    }

    // 전체 직원 수
    Long totalCount = queryFactory
        .select(employee.count())
        .from(employee)
        .where(builder)
        .fetchOne();

    if (totalCount == null || totalCount == 0L) {
      // TODO 총 직원이 0명일 때, 어떤 처리를 할지
      // 현재는 빈 리스트를 반환하는 것으로 해두었습니다.
      return Collections.emptyList();
    }

    // 필터링과 그룹화를 통한 반환
    // Projections.constructor은 생성자를 통해 DTO로 접근할 수 있습니다.
    return queryFactory.select(
            Projections.constructor(EmployeeDistributionDto.class,
                groupBy.equals("department") ? employee.department : employee.position,
                employee.count(),
                employee.count().multiply(100.0).divide(totalCount)
            )
        )
        .from(employee)
        .where(builder)
        .groupBy(groupBy.equals("department") ? employee.department : employee.position)
        .fetch();
  }

  private BooleanExpression buildCursorCondition( // 커서 기반 조건 생성
      Long idAfter,
      String cursor,
      String sortField,
      String sortDirection,
      QEmployee employee) {

    boolean isAsc = "asc".equalsIgnoreCase(sortDirection);
    BooleanExpression cursorCondition;

    switch (sortField) {
      case "name":
        // WHERE name > cursor OR name = cursor
        cursorCondition = employee.name.gt(cursor).or(employee.name.eq(cursor));
        break;
      case "employeeNumber":
        cursorCondition = employee.employeeNumber.gt(cursor).or(employee.employeeNumber.eq(cursor));
        break;
      case "hireDate":
        Instant hireDate = Instant.parse(cursor);
        cursorCondition = employee.hireDate.gt(hireDate).or(employee.hireDate.eq(hireDate));
        break;
      // 지정하지 않으면 name 이지만 잘못된 값을 전달했을시에,
      // 검증 로직을 서비스 단에서 구현하는 게 좋다곤 생각하나, 우선 추가해놓고 더 좋은 방안을 찾아보겠습니다.
      default:
        throw new IllegalArgumentException("sortField(" + sortField + ")는 존재하지 않습니다.");
    }

    return isAsc ? cursorCondition.and(employee.id.gt(idAfter))
        : cursorCondition.and(employee.id.lt(idAfter));
  }

  private OrderSpecifier<?> createOrderSpecifier(String sortField, String sortDirection,
      QEmployee employee) {
    Order direction = "asc".equalsIgnoreCase(sortDirection) ? Order.ASC : Order.DESC;

    switch (sortField) {
      case "name":
        // name 필드 기준 정렬
        return new OrderSpecifier<>(direction, employee.name);
      case "employeeNumber":
        // employeeNumber 필드 기준 정렬
        return new OrderSpecifier<>(direction, employee.employeeNumber);
      case "hireDate":
        // name 필드 기준 정렬
        return new OrderSpecifier<>(direction, employee.hireDate);
      default:
        return new OrderSpecifier<>(direction, employee.name);
    }
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
