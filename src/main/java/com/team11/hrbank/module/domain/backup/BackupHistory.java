package com.team11.hrbank.module.domain.backup;

import com.team11.hrbank.module.domain.file.File;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 백업 이력을 관리하는 엔티티
 */
@Entity
@Table(name = "backup_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BackupHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 백업을 실행한 작업자 (IP 주소 또는 'system') */
    @Column(nullable = false)
    private String worker;

    /** 백업 시작 시간 */
    @Column(nullable = false)
    private LocalDateTime startAt;

    /** 백업 종료 시간 */
    private LocalDateTime endedAt;

    /** 백업 상태 (진행중, 완료, 실패, 건너뜀) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupStatus status;

    /** 생성된 백업 파일 경로 */
    @ManyToOne
    @JoinColumn(name = "file_id", referencedColumnName = "id", nullable = true)
    private File file; // 백업된 파일 정보 (FK)
}
