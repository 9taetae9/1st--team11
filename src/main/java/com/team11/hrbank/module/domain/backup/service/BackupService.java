package com.team11.hrbank.module.domain.backup.service;

import com.team11.hrbank.module.common.dto.CursorPageResponse;
import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.backup.BackupHistory;
import com.team11.hrbank.module.domain.backup.BackupStatus;
import com.team11.hrbank.module.domain.backup.dto.BackupDto;
import com.team11.hrbank.module.domain.backup.mapper.BackupMapper;
import com.team11.hrbank.module.domain.backup.repository.BackupHistoryRepository;
import com.team11.hrbank.module.domain.backup.repository.BackupSpecifications;
import com.team11.hrbank.module.domain.backup.service.data.BackupDataService;
import com.team11.hrbank.module.domain.backup.service.file.BackupFileStorageService;
import com.team11.hrbank.module.domain.changelog.repository.ChangeLogRepository;
import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.service.FileService;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
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
    private final BackupDataService backupDataService;
    private final FileService fileService;
    private final ChangeLogRepository changeLogRepository;
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
            log.info("첫 백업 실행");
        }else{
            long changesCount = changeLogRepository.countByDateRangeFrom(lastBackupTime);
            isChanged = changesCount > 0;
            log.info("last backup time: {},. changes count: {}", lastBackupTime, changesCount);
        }

        //변경 사항 없는 경우 skip
        if(!isChanged){
            BackupHistory skippedHistory = saveBackupHistory(workerIp, BackupStatus.SKIPPED, null);
            log.info("백업 불필요 - SKIPPED 상태로 저장, ID: {}", skippedHistory.getId());
            return skippedHistory;
        }

        BackupHistory backupHistory = saveBackupHistory(workerIp, BackupStatus.IN_PROGRESS, null);
        log.info("백업 시작 - 이력 ID: {}", backupHistory.getId());

        File backupFile = null;
        try {

            String backupFilePath = fileStorageService.saveBackupToCsv(backupDataService.getAllDataForBackup());
            log.info("백업 파일 생성 완료: {}", backupFilePath);

            backupFile = createFileEntity(backupFilePath);

            backupHistory.setStatus(BackupStatus.COMPLETED);
            backupHistory.setEndedAt(Instant.now());
            backupHistory.setFile(backupFile);
            backupHistoryRepository.save(backupHistory);

            log.info("백업 완료 - 저장된 파일: {}", backupFilePath);
            return backupHistory;
        } catch (IOException e) {
            log.error("백업 실패", e);

            if (backupFile != null) {
                log.info("실패한 백업 파일 삭제 시도: {}", backupFile.getFilePath());
                fileService.deleteFile(backupFile);
            }

            try {
                String errorLogPath = fileStorageService.saveErrorLog(e);
                File errorLogFile = createFileEntity(errorLogPath);

                backupHistory.setStatus(BackupStatus.FAILED);
                backupHistory.setEndedAt(Instant.now());
                backupHistory.setFile(errorLogFile);
                backupHistoryRepository.save(backupHistory);

                log.info("백업 실패 - 에러 로그 저장: {}", errorLogPath);
            } catch (Exception ex) {
                log.error("에러 로그 저장 실패", ex);

                backupHistory.setStatus(BackupStatus.FAILED);
                backupHistory.setEndedAt(Instant.now());
                backupHistoryRepository.save(backupHistory);
            }

            throw new RuntimeException("백업 실패: " + e.getMessage(), e);
        }
    }

    private BackupHistory saveBackupHistory(String worker, BackupStatus status, File file) {
        BackupHistory backupHistory = new BackupHistory();
        backupHistory.setWorker(worker);
        backupHistory.setStartAt(Instant.now());
        backupHistory.setStatus(status);
        backupHistory.setFile(file);
        return backupHistoryRepository.save(backupHistory);
    }

    /**
     * 파일 엔티티 생성
     */
    private File createFileEntity(String filePath) throws IOException{
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("파일 경로가 유효하지 않습니다.");
        }

        java.io.File actualFile = new java.io.File(filePath);
        if (!actualFile.exists()) {
            throw new IOException("파일이 존재하지 않습니다: " + filePath);
        }

        File file = new File();
//        file.setFileName(filePath.substring(filePath.lastIndexOf("/") + 1));
        file.setFileName(Paths.get(filePath).getFileName().toString());
        file.setFilePath(filePath);

        String fileName = file.getFileName();
        int lastDotIndex = fileName.lastIndexOf('.');
        String format = lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toUpperCase() : "";
        file.setFormat(format);

        file.setSize(actualFile.length());
        return fileService.saveFile(file);
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


    /**
     *커서기반 페이징으로 백업 이력 조회
     */
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
        String entitySortField = mapSortField(sortField);

        // 정렬 방향 설정
        Sort sort = "DESC".equalsIgnoreCase(sortDirection) ?
            Sort.by(entitySortField).descending() :
            Sort.by(entitySortField).ascending();

        // 페이징 설정
        Pageable pageable = PageRequest.of(0, size, sort);

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

    private String mapSortField(String sortField) {
        if (sortField == null || sortField.equals("startedAt")) {
            return "startAt";
        }

        return sortField;
    }
}
