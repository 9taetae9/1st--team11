package com.team11.hrbank.module.domain.changelog.controller;

import com.team11.hrbank.module.domain.changelog.HistoryType;
import com.team11.hrbank.module.domain.changelog.dto.CursorPageResponseChangeLogDto;
import com.team11.hrbank.module.domain.changelog.service.ChangeLogService;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/change-logs")
@RequiredArgsConstructor
public class ChangeLogController {

  private final ChangeLogService changeLogService;


  @GetMapping//직원 정보 수정 이력 목록 조회. 상세 변경 내용은 포함 x
  public ResponseEntity<CursorPageResponseChangeLogDto> getAllChangeLogs(
      @RequestParam(required = false) String employeeNumber,
      @RequestParam(required = false) HistoryType type,
      @RequestParam(required = false) String memo,
      @RequestParam(required = false) String ipAddress,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant atFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant atTo,
      @RequestParam(required = false) Long idAfter, //이전 페이지 마지막 요소 id
      @RequestParam(required = false) String cursor, // 이전 페이지의 마지막 id
      @RequestParam(defaultValue = "10") int size, //페이지 크기
      @RequestParam(defaultValue = "at") String sortField, //정렬 필드(ipAddress, at)
      @RequestParam(defaultValue = "desc") String sortDirection) //정렬 방향 (asc, desc)
      throws UnknownHostException {

    InetAddress inetAddress = ipAddress != null ? InetAddress.getByName(ipAddress) : null;

    CursorPageResponseChangeLogDto response = changeLogService.getAllChangeLogs(employeeNumber,
        type, memo, inetAddress, atFrom, atTo,
        idAfter, cursor, size, sortField, sortDirection);

    return ResponseEntity.ok(response); //400 잘못된 요청 또는 지원하지 않는 정렬 필드
  }
}
