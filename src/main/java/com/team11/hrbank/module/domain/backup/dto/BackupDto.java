package com.team11.hrbank.module.domain.backup.dto;

import com.team11.hrbank.module.domain.backup.BackupHistory;
import com.team11.hrbank.module.domain.backup.BackupStatus;
import java.time.Instant;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 백업 정보를 반환하는 DTO
 */
public record BackupDto(
    Long id,
    String worker,
    Instant startedAt,
    Instant endedAt,
    BackupStatus status,
    Long fileId
) {}