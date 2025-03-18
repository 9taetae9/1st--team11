package com.team11.hrbank.module.domain.file.service;

import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.repository.FileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

  private final FileRepository fileRepository;
  private static final String STORAGE_PATH = System.getProperty("user.dir") + "/storage/files/";

  /**
   * 파일 업로드 처리
   *
   * @param file 업로드할 파일
   * @return 저장된 파일 엔티티
   * @throws IOException 파일 저장 중 발생한 예외
   */
  @Transactional
  public File uploadFile(MultipartFile file) throws IOException {
    // 파일 유효성 검사
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("업로드할 파일이 없습니다.");
    }

    String originalName = file.getOriginalFilename();
    if (originalName == null || originalName.trim().isEmpty()) {
      throw new IllegalArgumentException("파일 이름이 없습니다.");
    }

    log.info("파일 업로드 시작: 파일명={}, 크기={}bytes", originalName, file.getSize());

    // 저장 디렉토리 확인 및 생성
    Path storagePath = Paths.get(STORAGE_PATH);
    log.info("저장 경로: {}", storagePath.toAbsolutePath());

    if (!Files.exists(storagePath)) {
      try {
        Files.createDirectories(storagePath);
        log.info("저장 디렉토리 생성 완료: {}", storagePath.toAbsolutePath());
      } catch (IOException e) {
        log.error("저장 디렉토리 생성 실패: {}", e.getMessage());
        throw new IOException("파일 저장 디렉토리를 생성할 수 없습니다: " + e.getMessage());
      }
    }

    // 파일 확장자 추출
    String format = getExtension(originalName);

    // 고유한 파일명 생성 (현재 타임스탬프 추가)
    String timestamp = String.valueOf(System.currentTimeMillis());
    String uniqueFileName = timestamp + "_" + UUID.randomUUID().toString() + "_" + originalName;
    String filePath = STORAGE_PATH + uniqueFileName;

    log.debug("생성된 고유 파일명: {}", uniqueFileName);

    // 파일 저장
    Path path = Paths.get(filePath);

    // 같은 이름의 파일이 이미 존재하는지 확인 (덮어쓰기 방지)
    if (Files.exists(path)) {
      log.warn("같은 이름의 파일이 이미 존재합니다. 파일명 재생성을 시도합니다.");
      uniqueFileName =
          System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + "_" + originalName;
      filePath = STORAGE_PATH + uniqueFileName;
      path = Paths.get(filePath);

      if (Files.exists(path)) {
        log.error("파일명 재생성 후에도 같은 이름의 파일이 존재합니다.");
        throw new IOException("파일을 저장할 수 없습니다: 같은 이름의 파일이 이미 존재합니다.");
      }
    }

    try {
      // Files.copy 대신 transferTo 사용 (MultipartFile API 활용)
      java.io.File targetFile = path.toFile();
      // 상위 디렉토리가 없으면 생성
      if (!targetFile.getParentFile().exists()) {
        boolean dirCreated = targetFile.getParentFile().mkdirs();
        log.info("상위 디렉토리 생성 {}: {}",
            dirCreated ? "성공" : "실패", targetFile.getParentFile().getAbsolutePath());
      }

      file.transferTo(targetFile);
      log.info("파일 저장 성공: {}", filePath);
    } catch (IOException e) {
      log.error("파일 저장 실패: {} - {}", e.getClass().getName(), e.getMessage());
      throw new IOException("파일을 저장할 수 없습니다: " + e.getMessage());
    }

    // 파일 크기 확인
    long fileSize = file.getSize();
    if (fileSize <= 0) {
      try {
        fileSize = Files.size(path);
        log.debug("파일 크기 재계산: {}bytes", fileSize);
      } catch (IOException e) {
        log.warn("파일 크기를 얻을 수 없습니다: {}", e.getMessage());
        fileSize = 0;
      }
    }

    // File 엔티티 생성 및 저장
    File fileEntity = new File();
    fileEntity.setFileName(originalName);
    fileEntity.setFormat(format);
    fileEntity.setFilePath(filePath);
    fileEntity.setSize(fileSize);

    try {
      File savedFile = fileRepository.save(fileEntity);
      log.info("파일 메타데이터 저장 성공: ID={}, 파일명={}", savedFile.getId(), savedFile.getFileName());
      return savedFile;
    } catch (Exception e) {
      // DB 저장 실패 시 파일 삭제
      log.error("파일 메타데이터 저장 실패: {}", e.getMessage());
      try {
        Files.deleteIfExists(path);
        log.info("저장된 물리 파일 삭제 완료: {}", filePath);
      } catch (IOException ex) {
        log.warn("저장된 물리 파일 삭제 실패: {}", ex.getMessage());
      }
      throw e;
    }
  }

  /**
   * 파일 업데이트 (기존 파일 삭제 후 새 파일 업로드)
   *
   * @param oldFile     기존 파일 엔티티 (null 가능)
   * @param newFileData 새 파일 데이터
   * @return 업데이트된 파일 엔티티
   * @throws IOException 파일 처리 중 발생한 예외
   */
  @Transactional
  public File updateFile(File oldFile, MultipartFile newFileData) throws IOException {
    log.info("파일 업데이트 시작: oldFile={}, newFile={}",
        (oldFile != null) ? oldFile.getId() + " - " + oldFile.getFileName() : "없음",
        newFileData.getOriginalFilename());

    if (newFileData == null || newFileData.isEmpty()) {
      log.error("업로드할 새 파일이 null이거나 비어있습니다.");
      throw new IllegalArgumentException("업로드할 새 파일이 비어있습니다.");
    }

    // 기존 파일이 있으면 삭제
    if (oldFile != null) {
      log.info("기존 파일 삭제 시작: ID={}, 경로={}", oldFile.getId(), oldFile.getFilePath());
      try {
        boolean physicallyDeleted = deleteFilePhysically(oldFile.getFilePath());
        log.info("기존 파일 물리적 삭제 {}: {}",
            physicallyDeleted ? "성공" : "실패", oldFile.getFilePath());

        try {
          fileRepository.delete(oldFile);
          log.info("기존 파일 DB 레코드 삭제 성공: ID={}", oldFile.getId());
        } catch (Exception e) {
          log.error("기존 파일 DB 레코드 삭제 실패: {}", e.getMessage(), e);
          // DB 삭제 실패해도 계속 진행
        }
      } catch (Exception e) {
        log.error("기존 파일 삭제 중 오류 발생: {}", e.getMessage(), e);
        // 기존 파일 삭제 실패해도 계속 진행
      }
    }

    try {
      // 새 파일 업로드
      log.info("새 파일 업로드 시작: {}", newFileData.getOriginalFilename());
      File savedFile = uploadFile(newFileData);
      log.info("새 파일 업로드 성공: ID={}, 파일명={}", savedFile.getId(), savedFile.getFileName());
      return savedFile;
    } catch (IOException e) {
      log.error("새 파일 업로드 실패: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * ID로 파일 조회
   *
   * @param fileId 파일 ID
   * @return 조회된 파일 엔티티
   */
  public File getFileById(Long fileId) {
    if (fileId == null) {
      throw new IllegalArgumentException("파일 ID가 null입니다.");
    }
    return fileRepository.findById(fileId)
        .orElseThrow(() -> new ResourceNotFoundException("해당 파일이 존재하지 않습니다."));
  }

  /**
   * 파일 다운로드
   *
   * @param fileId 파일 ID
   * @return 파일 바이트 배열
   * @throws IOException 파일 읽기 중 발생한 예외
   */
  public byte[] downloadFile(Long fileId) throws IOException {
    File fileEntity = getFileById(fileId);
    Path filePath = Paths.get(fileEntity.getFilePath());

    if (!Files.exists(filePath)) {
      throw new ResourceNotFoundException("실제 파일이 존재하지 않습니다: " + fileEntity.getFilePath());
    }

    try {
      return Files.readAllBytes(filePath);
    } catch (IOException e) {
      log.error("파일 읽기 실패: {}", e.getMessage());
      throw new IOException("파일을 읽을 수 없습니다: " + e.getMessage());
    }
  }

  /**
   * 파일 삭제 (DB + 물리 파일)
   *
   * @param fileId 삭제할 파일 ID
   * @return 삭제 성공 여부
   */
  @Transactional
  public boolean deleteFile(Long fileId) {
    File fileEntity = getFileById(fileId);
    return deleteFile(fileEntity);
  }

  /**
   * 파일 엔티티로 파일 삭제 (DB + 물리 파일)
   *
   * @param fileEntity 삭제할 파일 엔티티
   * @return 삭제 성공 여부
   */
  @Transactional
  public boolean deleteFile(File fileEntity) {
    if (fileEntity == null) {
      return false;
    }

    String filePath = fileEntity.getFilePath();

    // DB에서 먼저 삭제
    try {
      fileRepository.delete(fileEntity);
    } catch (Exception e) {
      log.error("파일 메타데이터 삭제 실패: {}", e.getMessage());
      return false;
    }

    // 물리적 파일 삭제
    return deleteFilePhysically(filePath);
  }

  /**
   * 물리적 파일만 삭제
   *
   * @param filePath 파일 경로
   * @return 삭제 성공 여부
   */
  private boolean deleteFilePhysically(String filePath) {
    if (filePath == null || filePath.trim().isEmpty()) {
      return false;
    }

    try {
      Path path = Paths.get(filePath);
      if (Files.exists(path)) {
        Files.delete(path);
        log.info("파일 삭제 성공: {}", filePath);
        return true;
      } else {
        log.warn("삭제할 파일이 존재하지 않습니다: {}", filePath);
        return true; // 파일이 이미 없으면 성공으로 간주
      }
    } catch (IOException e) {
      log.error("파일 삭제 실패: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 파일 확장자 추출
   *
   * @param fileName 파일명
   * @return 파일 확장자
   */
  private String getExtension(String fileName) {
    if (fileName == null || fileName.trim().isEmpty()) {
      return "";
    }
    int idx = fileName.lastIndexOf(".");
    return (idx == -1) ? "" : fileName.substring(idx + 1);
  }
}
