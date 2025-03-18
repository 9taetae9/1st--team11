package com.team11.hrbank.module.domain.backup.dto;

import com.team11.hrbank.module.domain.backup.BackupHistory;
import com.team11.hrbank.module.domain.backup.BackupStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 백업 정보를 반환하는 DTO
 */
@Getter
public class BackupDto {
    private final Long id;
    private final String worker;
    private final LocalDateTime startAt;
    private final LocalDateTime endedAt;
    private final BackupStatus status;
    private final String filePath; // 파일 경로 추가

    public BackupDto(BackupHistory backupHistory) {
        this.id = backupHistory.getId();
        this.worker = backupHistory.getWorker();
        this.startAt = backupHistory.getStartAt();
        this.endedAt = Optional.ofNullable(backupHistory.getEndedAt()).orElse(null); // null-safe
        this.status = backupHistory.getStatus();

        // 🔹 `getFile()`이 null일 수도 있으므로 Optional 처리
        this.filePath = Optional.ofNullable(backupHistory.getFile())
                .map(file -> file.getFilePath())
                .orElse(""); // 파일 정보가 없으면 빈 문자열 반환
    }
}
