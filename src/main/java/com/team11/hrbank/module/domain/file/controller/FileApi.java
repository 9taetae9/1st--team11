package com.team11.hrbank.module.domain.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/files")
@Tag(name = "파일 관리", description = "파일 관리 API")
public interface FileApi {

  /**
   * 파일 다운로드 API
   * 파일을 다운로드하고 원본 파일명을 유지하여 반환.
   * Content-Type을 명확히 지정하여 OpenAPI 명세와 일치하도록 수정.
   *
   * @param id 파일 ID
   * @return 파일 바이트 데이터
   * @throws IOException 파일 읽기 중 예외 발생 시
   */
  @Operation(
      summary = "파일 다운로드",
      description = "파일을 다운로드합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "다운로드 성공",
          content = @Content(mediaType = "application/octet-stream")
      ),
      @ApiResponse(
          responseCode = "404",
          description = "파일을 찾을 수 없음",
          content = @Content(schema = @Schema(implementation = com.team11.hrbank.module.common.dto.ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 오류",
          content = @Content(schema = @Schema(implementation = com.team11.hrbank.module.common.dto.ErrorResponse.class))
      )
  })
  @GetMapping("/{id}/download")
  ResponseEntity<byte[]> downloadFile(
      @Parameter(description = "파일 ID", required = true)
      @PathVariable("id") long id) throws IOException;
}