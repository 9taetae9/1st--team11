package com.team11.hrbank.module.dashboard;

import com.team11.hrbank.module.dashboard.dto.DashboardResponse;
import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import com.team11.hrbank.module.domain.employee.dto.EmployeeTrendDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDistributionDto;
import com.team11.hrbank.module.domain.employee.service.EmployeeQueryService;
import com.team11.hrbank.module.domain.changelog.service.ChangeLogService;
import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeQueryService employeeQueryService;
    private final ChangeLogService changeLogService;
    // 백업 관련 선언 (백업 서비스가 있을 경우)
    // private final BackupService backupService;


    public DashboardResponse getDashboardData() {
        // 총 직원 수
        long totalEmployees = employeeQueryService.getEmployeeCount(EmployeeStatus.ACTIVE, null, null);
        if (totalEmployees == 0) {
            throw new ResourceNotFoundException("직원 데이터가 없습니다.");
        }

        // 이번 달 입사자 수
        long hiresThisMonth = employeeQueryService.getEmployeeCount(EmployeeStatus.ACTIVE, Instant.now().minus(30, ChronoUnit.DAYS), Instant.now());
        if (hiresThisMonth == 0) {
            throw new ResourceNotFoundException("이번 달 입사자 데이터가 없습니다.");
        }

        // 직원 수 추이
        List<EmployeeTrendDto> employeeTrend = employeeQueryService.getEmployeeTrend(null, null, "month");
        if (employeeTrend.isEmpty()) {
            throw new ResourceNotFoundException("최근 1년 월별 직원 수 추이가 없습니다.");
        }

        // 부서별 직원 분포
        List<EmployeeDistributionDto> departmentDist = employeeQueryService.getEmployeeDistribution("department", "ACTIVE");

        // 직무별 직원 분포
        List<EmployeeDistributionDto> positionDist = employeeQueryService.getEmployeeDistribution("position", "ACTIVE");

        // 최근 1주일 수정 이력 건수
        long changeLogsLastWeek = changeLogService.getChangeLogsCount(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now());

        // 백업 시간 (백업 서비스가 있을 경우 사용)
        // String lastBackupTime = backupService.getLatestBackupTime(); // 백업 서비스에서 최신 백업 시간 조회

        // 대시보드 응답 객체 생성
        return DashboardResponse.builder()
                .totalEmployees(totalEmployees)
                .changeLogsLastWeek(changeLogsLastWeek)
                .hiresThisMonth(hiresThisMonth)
                .employeeTrend(employeeTrend)
                .departmentDist(departmentDist)
                .positionDist(positionDist)
                .build();
    }
}
