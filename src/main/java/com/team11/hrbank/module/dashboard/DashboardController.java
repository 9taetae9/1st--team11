package com.team11.hrbank.module.dashboard;

import com.team11.hrbank.module.common.dto.ErrorResponse;
import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.dashboard.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드 관련 요청을 처리하는 컨트롤러
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 대시보드 데이터 조회 API
     */
    @PostMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboardData() {
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }

    // 예외 처리
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found",
                e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
}
