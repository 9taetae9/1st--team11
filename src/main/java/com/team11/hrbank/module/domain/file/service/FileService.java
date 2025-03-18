package com.team11.hrbank.module.domain.file.service;

import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.repository.FileRepository;
import com.team11.hrbank.module.common.exception.ResourceNotFoundException; // 추가: ResourceNotFoundException 사용
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j  // ✅ 로깅 프레임워크 적용
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private static final String STORAGE_PATH = "./storage/files/";

    /**
     * 파일 업로드 서비스
     * ✅ DTO 제거 후 MultipartFile 직접 사용
     *
     * 🚨 [Employee 팀 참고] 🚨
     * Employee 프로필 사진 업로드 시, 이 메서드를 호출하세요!
     * - `POST /api/employees`에서 MultipartFile을 받아 이 메서드에 전달
     * - 저장된 파일 정보를 Employee 엔티티에 매핑하는 방식으로 처리 필요
     */
    public void uploadFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("❌ 업로드할 파일이 없습니다.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new IllegalArgumentException("❌ 파일 이름이 없습니다.");
        }

        String format = getExtension(originalName);
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalName;
        String filePath = STORAGE_PATH + uniqueFileName;

        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent()); // 디렉토리 생성 (이미 존재하면 무시됨)
        file.transferTo(path.toFile()); // 파일 저장

        long fileSize = file.getSize();

        File fileEntity = new File();
        fileEntity.setFileName(originalName);
        fileEntity.setFormat(format);
        fileEntity.setFilePath(filePath);
        fileEntity.setSize(fileSize > 0 ? fileSize : 0);  // 크기 음수 방지

        // ✅ fileMapper.toEntity() 제거 → 바로 저장
        File savedFile = fileRepository.save(fileEntity);
        log.info("✅ 파일 업로드 성공: {}", savedFile.getFilePath());
    }

    /**
     * 파일 정보 조회
     * ✅ fileMapper.toEntity() 제거 → JPA 결과 그대로 반환
     * 수정: FileNotFoundException 대신 ResourceNotFoundException 사용하여 글로벌 예외 핸들러와 일치시킴
     */
    public File getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 파일이 존재하지 않습니다."));
    }

    /**
     * 파일 다운로드 서비스
     * 수정: 파일 존재 여부 체크 시 FileNotFoundException 대신 ResourceNotFoundException 사용
     */
    public byte[] downloadFile(Long fileId) throws Exception {
        File fileEntity = getFileById(fileId);
        Path filePath = Paths.get(fileEntity.getFilePath());

        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("실제 파일이 존재하지 않습니다.");
        }

        return Files.readAllBytes(filePath);
    }

    /**
     * 파일 확장자 추출 메서드
     */
    private String getExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return "";
        int idx = fileName.lastIndexOf(".");
        return (idx == -1) ? "" : fileName.substring(idx + 1);
    }
}
