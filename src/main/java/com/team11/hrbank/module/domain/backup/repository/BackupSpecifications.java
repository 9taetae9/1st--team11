package com.team11.hrbank.module.domain.backup.repository;


import com.team11.hrbank.module.domain.backup.BackupHistory;
import com.team11.hrbank.module.domain.backup.BackupStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 백업 이력 조회용
 */
public class BackupSpecifications {

  public static Specification<BackupHistory> withCriteria(
          String worker,
          BackupStatus status,
          Instant startedAtFrom,
          Instant startedAtTo,
          Long idAfter,
          Instant cursorTimestamp,
          String sortField,
          String sortDirection) {

    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      // 기본 필터 조건
      if (worker != null && !worker.isEmpty()) {
        predicates.add(criteriaBuilder.like(root.get("worker"), "%" + worker + "%"));
      }

      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }

      if (startedAtFrom != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startAt"), startedAtFrom));
      }

      if (startedAtTo != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startAt"), startedAtTo));
      }

      // 커서 조건 추가
      boolean isAscending = !"DESC".equalsIgnoreCase(sortDirection);

      if (cursorTimestamp != null) {
        // 타임스탬프 기반 커서
        if ("startAt".equals(sortField)) {
          if (isAscending) {
            predicates.add(criteriaBuilder.greaterThan(root.get("startAt"), cursorTimestamp));
          } else {
            predicates.add(criteriaBuilder.lessThan(root.get("startAt"), cursorTimestamp));
          }
        } else if ("endAt".equals(sortField)) {
          if (isAscending) {
            predicates.add(criteriaBuilder.greaterThan(root.get("endAt"), cursorTimestamp));
          } else {
            predicates.add(criteriaBuilder.lessThan(root.get("endAt"), cursorTimestamp));
          }
        }
      } else if (idAfter != null) {
        // ID 기반 커서
        if (isAscending) {
          predicates.add(criteriaBuilder.greaterThan(root.get("id"), idAfter));
        } else {
          predicates.add(criteriaBuilder.lessThan(root.get("id"), idAfter));
        }
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
