package com.team11.hrbank.module.dashboard.dto;

import com.team11.hrbank.module.domain.employee.dto.EmployeeDistributionDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeTrendDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 대시보드 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    /** 전체 직원 수 */
    private long totalEmployees;

    /** 최근 1주일 수정 이력 건수 */
    private long changeLogsLastWeek;

    /** 이번 달 입사자 수 */
    private long hiresThisMonth;

    /** 최근 1년 월별 직원 수 추이 */
    private List<EmployeeTrendDto> employeeTrend;

    /** 부서별 직원 분포 */
    private List<EmployeeDistributionDto> departmentDist;

    /** 직무별 직원 분포 */
    private List<EmployeeDistributionDto> positionDist;
}
