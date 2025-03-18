package com.team11.hrbank.module.domain.file.service;

import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.repository.FileRepository;
import com.team11.hrbank.module.common.exception.ResourceNotFoundException; 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j  
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private static final String STORAGE_PATH = "./storage/files/";

    
    public void uploadFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(" 업로드할 파일이 없습니다.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new IllegalArgumentException(" 파일 이름이 없습니다.");
        }

        String format = getExtension(originalName);
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalName;
        String filePath = STORAGE_PATH + uniqueFileName;

        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        file.transferTo(path.toFile()); 

        long fileSize = file.getSize();

        File fileEntity = new File();
        fileEntity.setFileName(originalName);
        fileEntity.setFormat(format);
        fileEntity.setFilePath(filePath);
        fileEntity.setSize(fileSize > 0 ? fileSize : 0);  

        //  fileMapper.toEntity() 제거 → 바로 저장
        File savedFile = fileRepository.save(fileEntity);
        log.info(" 파일 업로드 성공: {}", savedFile.getFilePath());
    }

   
    public File getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 파일이 존재하지 않습니다."));
    }

    
    public byte[] downloadFile(Long fileId) throws Exception {
        File fileEntity = getFileById(fileId);
        Path filePath = Paths.get(fileEntity.getFilePath());

        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("실제 파일이 존재하지 않습니다.");
        }

        return Files.readAllBytes(filePath);
    }

    
    private String getExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return "";
        int idx = fileName.lastIndexOf(".");
        return (idx == -1) ? "" : fileName.substring(idx + 1);
    }
}
