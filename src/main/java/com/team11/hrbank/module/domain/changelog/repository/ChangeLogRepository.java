package com.team11.hrbank.module.domain.changelog.repository;

import com.team11.hrbank.module.domain.changelog.ChangeLog;
import com.team11.hrbank.module.domain.changelog.HistoryType;
import java.net.InetAddress;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long>,
    JpaSpecificationExecutor<ChangeLog> {
  //시간 범위 조건
  @Query("select cl from ChangeLog as cl where "
      + "(:fromDate is null or cl.createdAt >= :fromDate) and"
      + "(:toDate is null or cl.createdAt <= :toDate)")
  long countByDateRange(
      @Param("fromDate") Instant fromDate,
      @Param("toDate") Instant toDate
  );
}
