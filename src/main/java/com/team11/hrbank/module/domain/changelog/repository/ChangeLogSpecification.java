package com.team11.hrbank.module.domain.changelog.repository;

import com.team11.hrbank.module.domain.changelog.ChangeLog;
import com.team11.hrbank.module.domain.changelog.HistoryType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ChangeLogSpecification {

  public static Specification<ChangeLog> withFilters(
      String employeeNumber,
      HistoryType type,
      String memo,
      InetAddress ipAddress,
      Instant fromDate,
      Instant toDate,
      Long idAfter,
      Instant cursorAt,
      String cursorIpAddress,
      String sortField,
      String sortDirection) {

    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (employeeNumber != null && !employeeNumber.isEmpty()) {
        predicates.add(criteriaBuilder.like(root.get("employeeNumber"), "%" + employeeNumber + "%"));
      }

      if (type != null) {
        predicates.add(criteriaBuilder.equal(root.get("type"), type));
      }

      if (memo != null && !memo.isEmpty()) {
        predicates.add(criteriaBuilder.like(root.get("memo"), "%" + memo + "%"));
      }

      if (ipAddress != null) {
        predicates.add(criteriaBuilder.equal(root.get("ipAddress"), ipAddress));
      }

      if (fromDate != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
      }

      if (toDate != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
      }

      if (idAfter != null) {
        predicates.add(criteriaBuilder.lessThan(root.get("id"), idAfter));
      }

      boolean isAsc = !"desc".equalsIgnoreCase(sortDirection);

      if ("at".equals(sortField) && cursorAt != null) {
        // 시간 기준 커서
        if (isAsc) {
          predicates.add(criteriaBuilder.greaterThan(root.get("createdAt"), cursorAt));
        } else {
          predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), cursorAt));
        }
      } else if ("ipAddress".equals(sortField) && cursorIpAddress != null) {
        // IP 주소 기준 커서 (String 비교)
        if (isAsc) {
          predicates.add(criteriaBuilder.greaterThan(root.get("ipAddress"), cursorIpAddress));
        } else {
          predicates.add(criteriaBuilder.lessThan(root.get("ipAddress"), cursorIpAddress));
        }
      } else if (idAfter != null) {
        // 프로토타입 기준 - IP 주소 정렬에서는 ID를 커서로 사용하는 것으로 보임
        if (isAsc) {
          predicates.add(criteriaBuilder.greaterThan(root.get("id"), idAfter));
        } else {
          predicates.add(criteriaBuilder.lessThan(root.get("id"), idAfter));
        }
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}