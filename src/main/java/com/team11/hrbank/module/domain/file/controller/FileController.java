package com.team11.hrbank.module.domain.file.controller;

import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.dto.request.FileUploadRequest;
import com.team11.hrbank.module.domain.file.dto.response.FileResponse;
import com.team11.hrbank.module.domain.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 파일 업로드
     * POST /api/files
     * multipart/form-data
     */
    @PostMapping
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        System.out.println("oliver486 uploadFile called");

        try {
            if (file.isEmpty()) {
                // 파일이 비었을 경우 예외 처리
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(FileResponse.builder()
                                .fileName("파일이 비었습니다")
                                .filePath("업로드할 파일이 없습니다.")
                                .build());
            }

            // 파일 업로드 요청 처리
            FileUploadRequest request = new FileUploadRequest(file);
            // size 정보도 처리하여 FileResponse 반환
            FileResponse response = fileService.uploadFile(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 예외 처리: 예외 메시지를 포함한 FileResponse 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileResponse.builder()
                            .fileName("파일 업로드 실패")
                            .filePath(e.getMessage())
                            .build());
        }
    }

    /**
     * 파일 다운로드
     * GET /api/files/{id}/download
     * 바이너리 데이터로 반환
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        try {
            // 파일 정보 조회 (원래 파일명 포함)
            File fileEntity = fileService.getFileById(id);
            if (fileEntity.getId() != null) {
                System.out.println("파일 가져오기 성공, ID: " + fileEntity.getId());
            } else {
                System.out.println("파일 가져오기 실패");
            }
            byte[] fileData = fileService.downloadFile(id);

            // 원래 파일명을 유지 (한글 깨짐 방지)
            String encodedFileName = URLEncoder.encode(fileEntity.getFileName(), StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
