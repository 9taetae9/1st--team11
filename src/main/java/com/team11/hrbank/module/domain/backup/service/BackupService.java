package com.team11.hrbank.module.domain.backup.service;

import com.team11.hrbank.module.domain.backup.BackupHistory;
import com.team11.hrbank.module.domain.backup.BackupStatus;
import com.team11.hrbank.module.domain.backup.repository.BackupHistoryRepository;
import com.team11.hrbank.module.domain.backup.service.file.BackupFileStorageService;
import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupHistoryRepository backupHistoryRepository;
    private final BackupFileStorageService fileStorageService;
    private final FileRepository fileRepository; // 파일 저장을 위한 Repository 추가

    @Transactional
    public void performBackup(String workerIp) {
        if (backupHistoryRepository.countInProgressBackups() > 0) {
            throw new IllegalStateException("이미 진행 중인 백업이 존재합니다.");
        }

        log.info("백업 실행 요청 받음 - 요청자 IP: {}", workerIp);

        LocalDateTime lastBackupTime = backupHistoryRepository.findLatestCompletedBackupTime();
        boolean isChanged = true; // TODO: 직원 데이터 변경 여부 확인 로직 추가 필요

        if (!isChanged) {
            BackupHistory skippedHistory = saveBackupHistory(workerIp, BackupStatus.SKIPPED, null);
            log.info("백업 불필요 - SKIPPED 상태로 저장됨, ID: {}", skippedHistory.getId());
            return;
        }

        BackupHistory backupHistory = saveBackupHistory(workerIp, BackupStatus.IN_PROGRESS, null);

        File backupFile = null;
        try {
            // STEP.3: CSV를 한 줄씩 저장 (OOM 방지)
            String backupFilePath = fileStorageService.saveBackupToCsv(generateEmployeeDataAsStream());

            // 파일 정보를 `files` 테이블에 저장
            backupFile = saveBackupFile(backupFilePath);

            // STEP.4-1: 백업 성공 처리
            backupHistory.setStatus(BackupStatus.COMPLETED);
            backupHistory.setEndedAt(LocalDateTime.now());
            backupHistory.setFile(backupFile);
            backupHistoryRepository.save(backupHistory);
            log.info("백업 완료 - 저장된 파일: {}", backupFilePath);
        } catch (IOException e) {
            // STEP.4-2: 백업 실패 처리
            if (backupFile != null) {
                fileStorageService.deleteFile(backupFile.getFilePath());
                fileRepository.delete(backupFile); // 파일 테이블에서 삭제
            }

            File errorLogFile = saveErrorLogFile(e);
            backupHistory.setStatus(BackupStatus.FAILED);
            backupHistory.setEndedAt(LocalDateTime.now());
            backupHistory.setFile(errorLogFile);
            backupHistoryRepository.save(backupHistory);
            log.error("백업 실패 - 에러 로그 저장 경로: {}", errorLogFile.getFilePath(), e);
            throw new RuntimeException("백업 실패", e);
        }
    }

    private BackupHistory saveBackupHistory(String worker, BackupStatus status, File file) {
        BackupHistory backupHistory = new BackupHistory();
        backupHistory.setWorker(worker);
        backupHistory.setStartAt(LocalDateTime.now());
        backupHistory.setStatus(status);
        backupHistory.setFile(file);
        return backupHistoryRepository.save(backupHistory);
    }

    private Stream<String> generateEmployeeDataAsStream() {
        return Stream.of(
                "ID,이름,이메일",
                "1,홍길동,hong@example.com"
                // TODO: 실제 직원 데이터를 DB에서 가져와서 스트림으로 변환
        );
    }

    /**
     * 백업된 파일 정보를 `files` 테이블에 저장
     */
    private File saveBackupFile(String filePath) {
        File file = new File();
        file.setFileName(filePath.substring(filePath.lastIndexOf("/") + 1));
        file.setFilePath(filePath);
        file.setFormat("CSV");
        file.setSize(new java.io.File(filePath).length());
        return fileRepository.save(file);
    }

    /**
     * 에러 로그 파일을 생성하고 `files` 테이블에 저장
     */
    private File saveErrorLogFile(Exception e) {
        String errorLogPath = fileStorageService.saveErrorLog(e);
        if (errorLogPath == null) {
            log.error("에러 로그 파일 저장 실패 - 원인: {}", e.getMessage(), e);
            return null;
        }
        return saveBackupFile(errorLogPath);
    }

    /**
     * 백업 이력 조회 메서드 (페이징 및 필터링 지원)
     * @param worker 작업자 (부분 일치)
     * @param status 백업 상태 (정확한 일치)
     * @param startedAtFrom 시작 시간 범위 (시작)
     * @param startedAtTo 시작 시간 범위 (종료)
     * @param idAfter 이전 페이지의 마지막 요소 ID (커서 기반 페이징)
     * @param pageable 페이지네이션 정보
     * @return 조건에 맞는 백업 이력을 Page 객체로 반환
     */
    public Page<BackupHistory> getFilteredBackups(String worker, BackupStatus status,
                                                  LocalDateTime startedAtFrom, LocalDateTime startedAtTo,
                                                  Long idAfter, Pageable pageable) {
        Specification<BackupHistory> spec = Specification.where(null);

        if (worker != null && !worker.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("worker"), "%" + worker + "%")
            );
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status)
            );
        }

        if (startedAtFrom != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("startAt"), startedAtFrom)
            );
        }

        if (startedAtTo != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("startAt"), startedAtTo)
            );
        }

        if (idAfter != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThan(root.get("id"), idAfter)
            );
        }

        return backupHistoryRepository.findAll(spec, pageable);
    }
}
