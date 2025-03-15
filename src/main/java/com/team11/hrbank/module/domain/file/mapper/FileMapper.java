package com.team11.hrbank.module.domain.file.mapper;

import com.team11.hrbank.module.domain.file.dto.response.FileResponse;
import com.team11.hrbank.module.domain.file.File;
import org.springframework.stereotype.Component;

@Component
public class FileMapper {

    public FileResponse toResponse(File entity) {
        return FileResponse.builder()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .format(entity.getFormat())
                .filePath(entity.getFilePath())
                .build();
    }
}
