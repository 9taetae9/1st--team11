package com.team11.hrbank.module.domain.file.service;

import com.team11.hrbank.module.common.config.FileStorageProperties;
import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.repository.FileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
  private final FileStorageProperties fileStorageProperties;
  private final FileRepository fileRepository;

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
    Path rootPath = Paths.get(fileStorageProperties.getRootPath());
    if (!Files.exists(rootPath)) {
      try {
        Files.createDirectories(rootPath);
        log.info("디렉토리 생성 완료: {}", rootPath);
      } catch (IOException e) {
        log.error("저장 디렉토리 생성 실패: {}", e.getMessage());
        throw new IOException("파일 저장 디렉토리를 생성할 수 없습니다: " + e.getMessage());
      }
    }

    // 파일 확장자 추출
    String format = FilenameUtils.getExtension(originalName);

    // 고유한 파일명 생성 (현재 타임스탬프 추가)
    String timestamp = String.valueOf(System.currentTimeMillis());
    String uniqueFileName = timestamp + "_" + UUID.randomUUID() + "_" + originalName;

    //파일 저장 경로 생성
    Path filePath = rootPath.resolve(uniqueFileName);

    try {
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
      log.info("파일 저장 성공: {}", filePath);
    } catch (IOException e) {
      log.error("파일 저장 실패: {}", e.getMessage(), e);
      throw new IOException("파일을 저장할 수 없습니다: " + e.getMessage(), e);
    }

    // File 엔티티 생성 및 저장
    File fileEntity = new File();
    fileEntity.setFileName(originalName);
    fileEntity.setFormat(format);
    fileEntity.setFilePath(filePath.toString());
    fileEntity.setSize(file.getSize());

    try {
      File savedFile = fileRepository.save(fileEntity);
      log.info("파일 메타데이터 저장 성공: ID={}, 파일명={}", savedFile.getId(), savedFile.getFileName());
      return savedFile;
    } catch (Exception e) {
      // DB 저장 실패 시 파일 삭제
      log.error("파일 메타데이터 저장 실패: {}", e.getMessage());
      deleteActualFile(filePath.toString());
      throw e;
    }
  }

  @Transactional
  public File saveFile(File fileEntity) {
    if (fileEntity == null) {
      throw new IllegalArgumentException("파일 엔티티가 null입니다.");
    }

    try {
      File savedFile = fileRepository.save(fileEntity);
      log.info("파일 메타데이터 저장 성공: id={}, 파일명={}", savedFile.getId(), savedFile.getFileName());
      return savedFile;
    } catch (Exception e) {
      log.error("파일 메타데이터 저장 실패: {}", e.getMessage(), e);
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

    if (oldFile != null) {
      deleteFile(oldFile);
    }

    return uploadFile(newFileData);
  }

  /**
   * ID로 파일 조회
   *
   * @param fileId 파일 ID
   * @return 조회된 파일 엔티티
   */
  @Transactional(readOnly = true)
  public File getFileById(Long fileId) {
    if (fileId == null) {
      throw new IllegalArgumentException("파일 ID가 null입니다.");
    }
    return fileRepository.findById(fileId)
        .orElseThrow(() -> ResourceNotFoundException.of("File", "fileId", fileId));
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
      throw ResourceNotFoundException.of("File","filePath",  fileEntity.getFilePath());
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
      log.info("파일 메타데이터 삭제 성고: ID={}", fileEntity.getId());
    } catch (Exception e) {
      log.error("파일 메타데이터 삭제 실패: {}", e.getMessage(), e);
      return false;
    }

    // 물리적 파일 삭제
    return deleteActualFile(filePath);
  }

  private boolean deleteActualFile(String filePath) {
    if (filePath == null || filePath.trim().isEmpty()) {
      return false;
    }

    try {
      Path path = Paths.get(filePath);
      if (Files.exists(path)) {
        Files.delete(path);
        log.error("파일 삭제 성공: {}", filePath);
        return true;
      } else {
        log.warn("삭제할 파일이 없습니다: {}", filePath);
        return true;
      }
    } catch (IOException e) {
      log.error("파일 삭제 실패: {}", e.getMessage(), e);
      return false;
    }
  }
}
