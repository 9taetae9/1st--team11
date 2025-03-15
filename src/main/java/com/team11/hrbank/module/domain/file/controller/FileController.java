package com.team11.hrbank.module.domain.file.controller;

import com.team11.hrbank.module.domain.file.dto.request.FileUploadRequest;
import com.team11.hrbank.module.domain.file.dto.response.FileResponse;
import com.team11.hrbank.module.domain.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            byte[] fileData = fileService.downloadFile(id);

            // 파일이 정상적으로 다운로드 가능할 때, HTTP 헤더에 파일 이름을 추가하여 반환
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"downloaded_file\"");
            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);

        } catch (Exception e) {
            // 예외 처리: 파일을 찾을 수 없는 경우 상태 404로 반환
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new byte[0]); // 파일을 찾을 수 없을 경우 빈 배열 반환
        }
    }
}
