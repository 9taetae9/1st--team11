package com.team11.hrbank.module.domain.file.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileResponse {
    private Long id;
    private String fileName;
    private String format;
    private String filePath;
    private Long size;  // size 필드 추가
}
