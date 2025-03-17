package com.team11.hrbank.module.domain.file.dto.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadRequest {
    private MultipartFile file;

    // 추가: 파일 크기
    public long getSize() {
        return file != null ? file.getSize() : 0L;
    }
}
