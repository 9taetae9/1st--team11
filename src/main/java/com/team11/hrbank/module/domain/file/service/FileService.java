package com.team11.hrbank.module.domain.file.service;

import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.dto.request.FileUploadRequest;
import com.team11.hrbank.module.domain.file.dto.response.FileResponse;
import com.team11.hrbank.module.domain.file.mapper.FileMapper;
import com.team11.hrbank.module.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileMapper fileMapper;

    private static final String STORAGE_PATH = "./storage/files/";

    /**
     * 파일 업로드
     * - 로컬에 파일 저장
     * - DB에 메타 정보 저장 (size 제외)
     */
    public FileResponse uploadFile(FileUploadRequest request) throws Exception {
        MultipartFile multipartFile = request.getFile();
        if (multipartFile.isEmpty()) {
            throw new Exception("업로드할 파일이 없습니다.");
        }

        // 원래 파일명 및 확장자 추출
        String originalName = multipartFile.getOriginalFilename();
        String format = getExtension(originalName);

        // 중복 방지를 위해 UUID 적용한 파일명 생성
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalName;
        String filePath = STORAGE_PATH + uniqueFileName;

        // 디렉토리가 없으면 생성 후 저장
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        multipartFile.transferTo(path.toFile());

        // DB에 저장
        File fileEntity = new File();
        fileEntity.setFileName(originalName);  // 원래 파일명 저장
        fileEntity.setFormat(format);
        fileEntity.setFilePath(filePath);

        fileRepository.save(fileEntity);
        return fileMapper.toResponse(fileEntity);
    }

    /**
     * 파일 정보 조회 (파일 ID로 검색)
     */
    public File getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("해당 파일이 존재하지 않습니다."));
    }

    /**
     * 파일 다운로드 (byte[] 반환)
     */
    public byte[] downloadFile(Long fileId) throws FileNotFoundException, Exception {
        File fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("해당 파일이 존재하지 않습니다."));

        Path filePath = Paths.get(fileEntity.getFilePath());
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("실제 파일이 존재하지 않습니다.");
        }

        return Files.readAllBytes(filePath);
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf(".");
        if (idx == -1) return "";
        return fileName.substring(idx + 1);
    }
}
