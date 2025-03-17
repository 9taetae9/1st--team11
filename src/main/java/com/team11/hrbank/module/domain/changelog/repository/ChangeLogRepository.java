package com.team11.hrbank.module.domain.changelog.repository;

import com.team11.hrbank.module.domain.changelog.ChangeLog;
import com.team11.hrbank.module.domain.changelog.HistoryType;
import java.net.InetAddress;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {

  @Query("select cl from ChangeLog as cl where "+
  "(:employeeNumber is null or cl.employeeNumber = :employeeNumber) and"
      + "(:type is null or cl.type = :type) and"
      + "(:memo is null or cl.memo like %:memo% ) and"
      + "(:ipAddress is null or cl.ipAddress = :ipAddress) and"
      + "(:fromDate is null or cl.createdAt >= :fromDate) and"
      + "(:toDate is null or cl.createdAt <= :toDate) and"
      + "(:idAfter is null or cl.id < :idAfter)"
      + "order by cl.id desc ")
  Page<ChangeLog> findAllWithFilters(
      @Param("employeeNumber") String employeeNumber,
      @Param("type") HistoryType type,
      @Param("memo") String memo,
      @Param("ipAddress") InetAddress ipAddress,
      @Param("fromDate") Instant fromDate,
      @Param("toDate") Instant toDate,
      @Param("idAfter") Long idAfter,
      Pageable pageable);

  //시간 범위 조건
  @Query("select cl from ChangeLog as cl where "
      + "(:fromDate is null or cl.createdAt >= :fromDate) and"
      + "(:toDate is null or cl.createdAt <= :toDate)")
  long countByDateRange(
      @Param("fromDate") Instant fromDate,
      @Param("toDate") Instant toDate
  );
}
