package com.team11.hrbank.module.domain.backup.controller;

import com.team11.hrbank.module.domain.backup.BackupHistory;
import com.team11.hrbank.module.domain.backup.BackupStatus;
import com.team11.hrbank.module.domain.backup.dto.BackupDto;
import com.team11.hrbank.module.domain.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    /**
     * 백업 실행 API
     * 클라이언트의 "X-Forwarded-For" 헤더에 담긴 IP 주소를 사용하여 백업을 실행합니다.
     * 동시 실행 체크를 통해 이미 진행 중인 백업이 있을 경우 409 Conflict 상태를 반환합니다.
     *
     * @param ipAddress 클라이언트의 IP 주소
     * @return 정상 실행 시 HTTP 200, 진행 중인 백업 존재 시 HTTP 409, 기타 오류 시 HTTP 500
     */
    @PostMapping
    public ResponseEntity<Void> triggerBackup(@RequestHeader("X-Forwarded-For") String ipAddress) {
        try {
            backupService.performBackup(ipAddress);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            // 진행 중인 백업이 있을 경우 Conflict 상태 반환
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            // 그 외 예외 발생 시 내부 서버 오류 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 백업 이력 조회 API
     * 작업자, 백업 상태, 시작 시간 범위, 그리고 커서 기반 페이지네이션을 위한 이전 페이지 마지막 요소 ID (idAfter)를 활용하여
     * 백업 이력을 필터링 및 페이징 처리합니다.
     *
     * @param worker 작업자 (부분 일치)
     * @param status 백업 상태 (정확한 일치)
     * @param startedAtFrom 시작 시간 범위의 시작 (ISO DATE-TIME 형식)
     * @param startedAtTo 시작 시간 범위의 종료 (ISO DATE-TIME 형식)
     * @param idAfter 이전 페이지 마지막 요소의 ID (커서 기반 페이징)
     * @param size 페이지 크기 (기본값: 10)
     * @param sortField 정렬 필드 (기본값: startAt)
     * @param sortDirection 정렬 방향 (DESC 또는 ASC, 기본값: DESC)
     * @return 조건에 맞게 필터링된 백업 이력 목록을 BackupDto로 변환하여 반환
     */
    @GetMapping
    public ResponseEntity<Page<BackupDto>> getBackupHistories(
            @RequestParam(required = false) String worker,
            @RequestParam(required = false) BackupStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedAtFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedAtTo,
            @RequestParam(required = false) Long idAfter, // 커서 기반 페이징을 위한 파라미터
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "startAt") String sortField,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection) {

        // Pageable 객체 생성: 페이지 번호는 0으로 고정하고, size와 정렬 조건을 적용합니다.
        Pageable pageable = PageRequest.of(0, size,
                sortDirection.equalsIgnoreCase("DESC") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

        // 서비스 메서드를 호출하여 필터 조건과 페이징 정보를 전달하고 결과를 조회합니다.
        Page<BackupHistory> backupHistories = backupService.getFilteredBackups(worker, status, startedAtFrom, startedAtTo, idAfter, pageable);
        // 조회된 백업 이력을 DTO 객체로 변환 후 반환합니다.
        Page<BackupDto> backupDtos = backupHistories.map(BackupDto::new);
        return ResponseEntity.ok(backupDtos);
    }
}
