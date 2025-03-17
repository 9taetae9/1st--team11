package com.team11.hrbank.module.domain.backup.service.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * 백업 데이터를 CSV 파일로 저장하는 서비스 (OOM 방지 적용)
 */
@Slf4j
@Service
public class BackupFileStorageService {
    private static final String BACKUP_DIR = "./storage/files/backups/";
    private static final String ERROR_LOG_DIR = "./storage/files/logs/";

    static {
        new File(BACKUP_DIR).mkdirs(); // 디렉토리 자동 생성
        new File(ERROR_LOG_DIR).mkdirs();
    }

    /**
     * 대용량 데이터를 효율적으로 CSV로 저장하는 방식 (Stream 지원)
     * @param backupDataStream 스트림 형태의 백업 데이터
     * @return 저장된 파일 경로
     * @throws IOException 파일 저장 실패 시 예외 발생
     */
    public String saveBackupToCsv(Stream<String> backupDataStream) throws IOException {
        String filePath = BACKUP_DIR + "backup_" + System.currentTimeMillis() + ".csv";

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            writer.write("\uFEFF"); // BOM 추가 (Excel 한글 깨짐 방지)

            backupDataStream.forEach(line -> {
                try {
                    writer.write(line);
                    writer.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        return filePath;
    }

    /**
     * 파일 삭제 메서드
     * @param filePath 삭제할 파일 경로
     */
    public void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.delete()) {
            log.info("파일 삭제 완료: {}", filePath);
        } else {
            log.warn("파일 삭제 실패 또는 존재하지 않음: {}", filePath);
        }
    }

    /**
     * 백업 실패 시 에러 로그를 저장하는 메서드 (로그 저장 실패 방지)
     */
    public String saveErrorLog(Exception e) {
        String errorFilePath = ERROR_LOG_DIR + "error_log_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".log";

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFilePath), StandardCharsets.UTF_8))) {
            writer.write("[ERROR] 백업 실패: " + LocalDateTime.now());
            writer.newLine();
            writer.write(e.getMessage());
            writer.newLine();
            for (StackTraceElement el : e.getStackTrace()) {
                writer.write(el.toString());
                writer.newLine();
            }
        } catch (IOException ioException) {
            log.error("에러 로그 파일 저장 실패", ioException);
            return null;
        }
        return errorFilePath;
    }
}
