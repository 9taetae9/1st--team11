package com.team11.hrbank.module.domain.backup.service;

import com.team11.hrbank.module.common.dto.CursorPageResponse;
import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.backup.BackupHistory;
import com.team11.hrbank.module.domain.backup.BackupStatus;
import com.team11.hrbank.module.domain.backup.dto.BackupDto;
import com.team11.hrbank.module.domain.backup.mapper.BackupMapper;
import com.team11.hrbank.module.domain.backup.repository.BackupHistoryRepository;
import com.team11.hrbank.module.domain.backup.repository.BackupSpecifications;
import com.team11.hrbank.module.domain.backup.service.file.BackupFileStorageService;
import com.team11.hrbank.module.domain.changelog.repository.ChangeLogRepository;
import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.repository.FileRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupHistoryRepository backupHistoryRepository;
    private final BackupFileStorageService fileStorageService;
    private final FileRepository fileRepository; // 파일 저장을 위한 Repository 추가
    private final ChangeLogRepository changeLogRepository;
//    private final EmployeeRepository employeeRepository;

    private final BackupMapper backupMapper;

    /**
     * 백업을 실행하고 결과를 반환
     * @param workerIp 작업자 ip 또는 system
     * @return 생성된 백업 이력
     */
    @Transactional
    public BackupHistory performBackup(String workerIp) {
        if (backupHistoryRepository.countInProgressBackups() > 0) {
            throw new IllegalStateException("이미 진행 중인 백업이 존재합니다.");
        }

        log.info("백업 실행 요청 받음 - 요청자 IP: {}", workerIp);

        Instant lastBackupTime = backupHistoryRepository.findLatestCompletedBackupTime();


        boolean isChanged;
        if (lastBackupTime == null) {
            isChanged = true;
            log.info("첫 백업");
        }else{
            long changesCount = changeLogRepository.countByDateRangeFrom(lastBackupTime);
            isChanged = changesCount > 0;
            log.info("last backup time: {},. changes count: {}", lastBackupTime, changesCount);
        }

        if(!isChanged){
            BackupHistory skippedHistory = saveBackupHistory(workerIp, BackupStatus.SKIPPED, null);
            log.info("백업 불필요 - SKIPPED 상태로 저장됨, ID: {}", skippedHistory.getId());
            return skippedHistory;
        }

        BackupHistory backupHistory = saveBackupHistory(workerIp, BackupStatus.IN_PROGRESS, null);

        File backupFile = null;
        try {
            //csv 생성 및 저장
            String backupFilePath = fileStorageService.saveBackupToCsv(generateEmployeeDataAsStream());

            // 파일 정보를 `files` 테이블에 저장
            backupFile = saveBackupFile(backupFilePath);

            // STEP.4-1: 백업 성공 처리
            backupHistory.setStatus(BackupStatus.COMPLETED);
            backupHistory.setEndedAt(Instant.now());
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
            backupHistory.setEndedAt(Instant.now());
            backupHistory.setFile(errorLogFile);
            backupHistoryRepository.save(backupHistory);
            log.error("백업 실패 - 에러 로그 저장 경로: {}", errorLogFile.getFilePath(), e);
            throw new RuntimeException("백업 실패", e);
        }

        return backupHistory;
    }

    private BackupHistory saveBackupHistory(String worker, BackupStatus status, File file) {
        BackupHistory backupHistory = new BackupHistory();
        backupHistory.setWorker(worker);
        backupHistory.setStartAt(Instant.now());
        backupHistory.setStatus(status);
        backupHistory.setFile(file);
        return backupHistoryRepository.save(backupHistory);
    }


    //전체 직원 정보 처리
//    private Stream<String> generateEmployeeDataAsStream() {
//        String header = "ID,직원번호,이름,이메일,부서,직급,입사일,상태,생성일";
//    }

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
                                                  Instant startedAtFrom, Instant startedAtTo,
                                                  Long idAfter, Pageable pageable) {

        return backupHistoryRepository.findAll(
            BackupSpecifications.withCriteria(worker, status, startedAtFrom, startedAtTo, idAfter),
                                            pageable);
    }

    /**
     * 지정된 상태의 최근 백업 조회
     * @param status 백업 상태
     * @return 가장 최근 백업 이력
     */
    @Transactional(readOnly = true)
    public BackupHistory getLatestBackupByStatus(BackupStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("backupStatus can not be null");
        }

        return backupHistoryRepository.findTopByStatusOrderByStartAtDesc(status)
            .orElseThrow(() -> ResourceNotFoundException.of(
                "BackupHistory", "status", status.toString()));
    }


    @Transactional(readOnly = true)
    public CursorPageResponse<BackupDto> getBackupHistoriesWithCursor(
        String worker, BackupStatus status,
        Instant startedAtFrom, Instant startedAtTo,
        Long idAfter, String cursor, int size,
        String sortField, String sortDirection) {

        // 커서에서 ID 추출
        if (cursor != null && !cursor.isEmpty() && idAfter == null) {
            idAfter = CursorPageResponse.extractIdFromCursor(cursor);
        }

        // 정렬 필드 매핑
        String entitySortField = "startedAt".equals(sortField) ? "startAt" : sortField;

        // 페이징 설정
        Pageable pageable = PageRequest.of(0, size,
            sortDirection.equalsIgnoreCase("DESC") ?
                Sort.by(entitySortField).descending() :
                Sort.by(entitySortField).ascending());

        // 백업 이력 조회
        Page<BackupHistory> backupHistories = getFilteredBackups(
            worker, status, startedAtFrom, startedAtTo, idAfter, pageable);

        // DTO 변환
        List<BackupDto> backupDtos = backupMapper.toDtoList(backupHistories.getContent());

        // 마지막 ID 추출
        Long lastId = backupHistories.getContent().isEmpty() ?
            null : backupHistories.getContent().get(backupHistories.getContent().size() - 1).getId();

        // 커서 페이지 응답 생성
        return CursorPageResponse.of(
            backupDtos,
            lastId,
            size,
            backupHistories.getTotalElements());
    }
}
