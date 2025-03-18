package com.team11.hrbank.module.domain.employee.service;

import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.changelog.service.ChangeLogService;
import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import com.team11.hrbank.module.domain.employee.dto.CursorPageResponseEmployeeDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDistributionDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeTrendDto;
import com.team11.hrbank.module.domain.employee.mapper.EmployeeMapper;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepository;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepositoryCustom;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeQueryService {


  private final EmployeeRepository employeeRepository;
  private final EmployeeRepositoryCustom employeeRepositoryCustom;
  private final EmployeeMapper employeeMapper;
  private final ChangeLogService changeLogService;

  // 부서 별 직원 수
  public Long countByDepartmentId(Long departmentId) {
    return employeeRepository.countByDepartmentId(departmentId);
  }

  // 직원 상세 조회
  public EmployeeDto getEmployeeDetails(Long id) {
    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("employee(" + id + ")는 존재하지 않습니다."));
    return employeeMapper.toDto(employee);
  }

  // 직원 목록 조회
  public CursorPageResponseEmployeeDto getListEmployees(
      String nameOrEmail,
      String employeeNumber,
      String departmentName,
      String position,
      Instant hireDateFrom,
      Instant hireDateTo,
      EmployeeStatus status,
      Long idAfter,
      String cursor,
      Integer size,
      String sortField,
      String sortDirection
  ) {

    List<Employee> employees = employeeRepositoryCustom.findEmployeesByConditions(
        nameOrEmail,
        employeeNumber,
        departmentName,
        position,
        hireDateFrom,
        hireDateTo,
        status,
        idAfter,
        cursor,
        size + 1,
        sortField,
        sortDirection);

    if (employees.isEmpty()) {
      throw new ResourceNotFoundException("해당 조건에 맞는 직원이 존재하지 않습니다.");
    }

    String nextCursor = null;
    Long nextIdAfter;
    boolean hasNext = false;

    // hasNext 간단 구현 :
    // 별도의 쿼리 메서드 사용없이 size+1 를 전달하여 구현한다.
    // employees.size() > size = 한 페이지의 사이즈가 전달된 사이즈보다 클 경우, next가 있다고 판단한다.
    if (employees.size() > size) {
      // size + 1로 조회했기 때문에, 마지막 직원은 실제 목록에 포함되지 않는다.
      employees.remove(employees.size() - 1);  // 마지막 직원 제거
      hasNext = true;
    }

    Employee lastEmployee = employees.get(employees.size() - 1);

    // cursor 값이 없으면 sortField 값에 맞춰 기본값을 설정
    if (cursor == null || cursor.isEmpty()) {
      // 기본적으로 sortField가 "name"으로 설정되어 있으므로 name을 기준으로 페이지네이션 처리
      cursor = sortField;
    }

    switch (cursor) {
      case "name":
        nextCursor = lastEmployee.getName();
        break;
      case "employeeNumber":
        nextCursor = lastEmployee.getEmployeeNumber();
        break;
      case "hireDate":
        nextCursor = lastEmployee.getHireDate().toString();
        break;
    }

    nextIdAfter = lastEmployee.getId();

    return new CursorPageResponseEmployeeDto(
        employees.stream()
            .map(employeeMapper::toDto).toList(),
        nextCursor,
        nextIdAfter,
        employees.size(),
        getEmployeeCount(status, hireDateFrom, hireDateTo),
        hasNext
    );
  }

  // 직원 분포 조회
  public List<EmployeeDistributionDto> getEmployeeDistribution(String groupBy, String status) {
    return employeeRepositoryCustom.findEmployeeDistribution(groupBy,
        EmployeeStatus.valueOf(status));
  }

  // TODO -----------------------------------------------------------------------------------------------
  // 직원 수 추이
  public List<EmployeeTrendDto> getEmployeeTrend(Instant from, Instant to, String periodType) {
    List<EmployeeTrendDto> stats = new ArrayList<>();

    // 기본값 설정: from과 to가 null일 경우 최근 12개월 데이터 반환
    if (from == null) {
      from = ZonedDateTime.now().minus(12, ChronoUnit.MONTHS).toInstant(); // 기본값: 최근 12개월
    }
    if (to == null) {
      to = ZonedDateTime.now().toInstant(); // 기본값: 현재 시간
    }

    switch (periodType.toLowerCase()) {
      case "day":
        stats = getDailyStats(from, to);
        break;
      case "month":
        stats = getMonthlyStats(from, to);
        break;
      case "quarter":
        stats = getQuarterlyStats(from, to);
        break;
      case "year":
        stats = getYearlyStats(from, to);
        break;
      case "week":
        stats = getWeeklyStats(from, to);
        break;
      default:
        throw new IllegalArgumentException("Invalid period type");
    }

    return stats;
  }

  private List<EmployeeTrendDto> getDailyStats(Instant from, Instant to) {
    List<EmployeeTrendDto> dailyStats = new ArrayList<>();
    ZonedDateTime current = ZonedDateTime.ofInstant(from, ZoneId.of("UTC"));
    ZonedDateTime end = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));

    while (current.isBefore(end) || current.isEqual(end)) {
      ZonedDateTime nextDay = current.plusDays(1);
      long currentCount = employeeRepository.countEmployeesByHireDateBetween(current.toInstant(),
          nextDay.toInstant());

      // 이전 날의 데이터를 구하기 위한 코드
      ZonedDateTime previousDay = current.minusDays(1);
      long previousCount = employeeRepository.countEmployeesByHireDateBetween(
          previousDay.toInstant(), current.toInstant());

      long change = currentCount - previousCount;
      double changeRate = previousCount > 0 ? (double) change / previousCount * 100 : 0.0;

      dailyStats.add(new EmployeeTrendDto(current.toString(), currentCount, change, changeRate));

      current = nextDay;
    }

    return dailyStats;
  }

  // 주별 데이터 처리
  private List<EmployeeTrendDto> getWeeklyStats(Instant from, Instant to) {
    List<EmployeeTrendDto> weeklyStats = new ArrayList<>();
    ZonedDateTime current = ZonedDateTime.ofInstant(from, ZoneId.of("UTC"));
    ZonedDateTime end = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));

    // 주 시작일을 기준으로 진행
    while (current.isBefore(end) || current.isEqual(end)) {
      ZonedDateTime startOfWeek = current.with(DayOfWeek.MONDAY);
      ZonedDateTime endOfWeek = startOfWeek.plusWeeks(1);

      long currentCount = employeeRepository.countEmployeesByHireDateBetween(
          startOfWeek.toInstant(), endOfWeek.toInstant());

      // 이전 주의 데이터를 구하기 위한 코드
      ZonedDateTime previousWeekStart = startOfWeek.minusWeeks(1);
      ZonedDateTime previousWeekEnd = previousWeekStart.plusWeeks(1);
      long previousCount = employeeRepository.countEmployeesByHireDateBetween(
          previousWeekStart.toInstant(), previousWeekEnd.toInstant());

      long change = currentCount - previousCount;
      double changeRate = previousCount > 0 ? (double) change / previousCount * 100 : 0.0;

      weeklyStats.add(new EmployeeTrendDto(
          current.getYear() + "-W" + current.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
          currentCount, change, changeRate));

      current = endOfWeek;
    }

    return weeklyStats;
  }

  // 월별 데이터 처리
  private List<EmployeeTrendDto> getMonthlyStats(Instant from, Instant to) {
    List<EmployeeTrendDto> monthlyStats = new ArrayList<>();
    ZonedDateTime current = ZonedDateTime.ofInstant(from, ZoneId.of("UTC"));
    ZonedDateTime end = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));

    while (current.isBefore(end) || current.isEqual(end)) {
      ZonedDateTime startOfMonth = current.withDayOfMonth(1);
      ZonedDateTime endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

      long currentCount = employeeRepository.countEmployeesByHireDateBetween(
          startOfMonth.toInstant(), endOfMonth.toInstant());

      // 이전 달의 데이터를 구하기 위한 코드
      ZonedDateTime previousMonthStart = startOfMonth.minusMonths(1);
      ZonedDateTime previousMonthEnd = previousMonthStart.plusMonths(1).minusDays(1);
      long previousCount = employeeRepository.countEmployeesByHireDateBetween(
          previousMonthStart.toInstant(), previousMonthEnd.toInstant());

      long change = currentCount - previousCount;
      double changeRate = previousCount > 0 ? (double) change / previousCount * 100 : 0.0;

      monthlyStats.add(new EmployeeTrendDto(
          current.getYear() + "." + String.format("%02d", current.getMonthValue()),
          currentCount, change, changeRate));

      // 다음 월로 갱신
      current = startOfMonth.plusMonths(1);
    }

    return monthlyStats;
  }

  // 분기별 데이터 처리
  private List<EmployeeTrendDto> getQuarterlyStats(Instant from, Instant to) {
    List<EmployeeTrendDto> quarterlyStats = new ArrayList<>();
    ZonedDateTime current = ZonedDateTime.ofInstant(from, ZoneId.of("UTC"));
    ZonedDateTime end = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));

    while (current.isBefore(end) || current.isEqual(end)) {
      ZonedDateTime startOfQuarter = current.withMonth(((current.getMonthValue() - 1) / 3) * 3 + 1)
          .withDayOfMonth(1);
      ZonedDateTime endOfQuarter = startOfQuarter.plusMonths(3).minusDays(1);

      long currentCount = employeeRepository.countEmployeesByHireDateBetween(
          startOfQuarter.toInstant(), endOfQuarter.toInstant());

      // 이전 분기의 데이터를 구하기 위한 코드
      ZonedDateTime previousQuarterStart = startOfQuarter.minusMonths(3);
      ZonedDateTime previousQuarterEnd = previousQuarterStart.plusMonths(3).minusDays(1);
      long previousCount = employeeRepository.countEmployeesByHireDateBetween(
          previousQuarterStart.toInstant(), previousQuarterEnd.toInstant());

      long change = currentCount - previousCount;
      double changeRate = previousCount > 0 ? (double) change / previousCount * 100 : 0.0;

      quarterlyStats.add(new EmployeeTrendDto(
          current.getYear() + "-Q" + ((current.getMonthValue() - 1) / 3 + 1),
          currentCount, change, changeRate));

      current = endOfQuarter.plusDays(1);
    }

    return quarterlyStats;
  }

  // 연도별 데이터 처리
  private List<EmployeeTrendDto> getYearlyStats(Instant from, Instant to) {
    List<EmployeeTrendDto> yearlyStats = new ArrayList<>();
    ZonedDateTime current = ZonedDateTime.ofInstant(from, ZoneId.of("UTC"));
    ZonedDateTime end = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));

    while (current.isBefore(end) || current.isEqual(end)) {
      ZonedDateTime startOfYear = current.withDayOfYear(1);
      ZonedDateTime endOfYear = startOfYear.plusYears(1).minusDays(1);

      long currentCount = employeeRepository.countEmployeesByHireDateBetween(
          startOfYear.toInstant(), endOfYear.toInstant());

      // 이전 년도의 데이터를 구하기 위한 코드
      ZonedDateTime previousYearStart = startOfYear.minusYears(1);
      ZonedDateTime previousYearEnd = previousYearStart.plusYears(1).minusDays(1);
      long previousCount = employeeRepository.countEmployeesByHireDateBetween(
          previousYearStart.toInstant(), previousYearEnd.toInstant());

      long change = currentCount - previousCount;
      double changeRate = previousCount > 0 ? (double) change / previousCount * 100 : 0.0;

      yearlyStats.add(new EmployeeTrendDto(
          current.getYear() + "",
          currentCount, change, changeRate));

      current = endOfYear.plusDays(1);
    }

    return yearlyStats;
  }

  // TODO ------------------------------------------------------------------------------------------------

  // 직원 수 조회
  public long getEmployeeCount(EmployeeStatus status, Instant fromDate, Instant toDate) {
    return employeeRepositoryCustom.countByStatusAndHireDateBetween(status, fromDate, toDate);
  }

}
