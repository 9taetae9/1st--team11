package com.team11.hrbank.module.domain.employee.service;

import com.team11.hrbank.module.common.dto.CursorPageResponse;
import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.changelog.service.ChangeLogService;
import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.EmployeeStatus;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
        .orElseThrow(() -> ResourceNotFoundException.of("Employee", "id", id));
    return employeeMapper.toDto(employee);
  }

  // 직원 목록 조회
  public CursorPageResponse<EmployeeDto> getListEmployees(
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
    if (cursor != null && !cursor.isEmpty() && idAfter == null) {
      idAfter = CursorPageResponse.extractIdFromCursor(cursor);
    }

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

    Long lastId = null;

    if (employees.size() > size) {
      // size + 1로 조회했기 때문에, 마지막 직원은 실제 목록에 포함되지 않는다.
      employees.remove(employees.size() - 1);  // 마지막 직원 제거
    }

    if (!employees.isEmpty()) {
      lastId = employees.get(employees.size() - 1).getId();
    }

    List<EmployeeDto> employeeDtos = employees.stream().map(employeeMapper::toDto).toList();

    long totalCount = getEmployeeCount(status, hireDateFrom, hireDateTo);

    return CursorPageResponse.of(employeeDtos, lastId, size, totalCount);
  }

  // 직원 분포 조회
  public List<EmployeeDistributionDto> getEmployeeDistribution(String groupBy, String status) {

    EmployeeStatus employeeStatus = null;
    if (status != null && !status.isEmpty()) {
      try {
        employeeStatus = EmployeeStatus.valueOf(status);
      } catch (IllegalArgumentException e) {
        log.warn("유효하지 않은 상태 값입니다: {}", status);
      }
    }

    return employeeRepositoryCustom.findEmployeeDistribution(groupBy,employeeStatus);
  }

  // 직원 수 추이
  public List<EmployeeTrendDto> getEmployeeTrend(Instant from, Instant to, String periodType) {
    // 기본값 설정: from과 to가 null일 경우 최근 12개월 데이터 반환
    if (from == null) {
      from = ZonedDateTime.now().minus(12, ChronoUnit.MONTHS).toInstant();
    }
    if (to == null) {
      to = Instant.now();
    }

    return switch (periodType.toLowerCase()){
      case "day" -> getDailyStats(from, to);
      case "week" -> getWeeklyStats(from, to);
      case "month" -> getMonthlyStats(from, to);
      case "quarter" -> getQuarterlyStats(from, to);
      case "year" -> getYearlyStats(from, to);
      default -> throw new IllegalArgumentException("올바르지 않은 시간 단위입니다:" + periodType);
    };
  }

  //일별 통계 조회
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

      String formattedDate = current.format(DateTimeFormatter.ISO_LOCAL_DATE);
      formattedDate = safeDateFormat(formattedDate, current.getYear() + "-" +
          String.format("%02d", current.getMonthValue()) + "-" +
          String.format("%02d", current.getDayOfMonth()));

      dailyStats.add(new EmployeeTrendDto(formattedDate, currentCount, change,
          Math.round(changeRate * 100) / 100.0));

      current = nextDay;
    }

    return dailyStats;
  }

  // 주별 데이터 처리
  private List<EmployeeTrendDto> getWeeklyStats(Instant from, Instant to) {
    List<EmployeeTrendDto> weeklyStats = new ArrayList<>();
    ZonedDateTime current = ZonedDateTime.ofInstant(from, ZoneId.of("UTC"));
    ZonedDateTime end = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));

    //주 시작일을 수요일로 조정(프로토타입과 일치)
    current = current.with(DayOfWeek.WEDNESDAY);

    // 주 시작일을 기준으로 진행
    while (current.isBefore(end) || current.isEqual(end)) {
      ZonedDateTime endOfWeek = current.plusWeeks(1);

      long currentCount = employeeRepository.countEmployeesByHireDateBetween(
          current.toInstant(), endOfWeek.toInstant());

      // 지난주 데이터
      ZonedDateTime previousWeekStart = current.minusWeeks(1);
      long previousCount = employeeRepository.countEmployeesByHireDateBetween(
          previousWeekStart.toInstant(), current.toInstant());

      long change = currentCount - previousCount;
      double changeRate = previousCount > 0 ? (double) change / previousCount * 100 : 0.0;

      // 차트가 인식할 수 있는 ISO 날짜 형식 사용 (주 시작일)
      String isoDate = current.format(DateTimeFormatter.ISO_LOCAL_DATE); // YYYY-MM-DD 형식

      weeklyStats.add(new EmployeeTrendDto(
          isoDate, // ISO 날짜 사용 (차트가 인식 가능)
          currentCount,
          change,
          Math.round(changeRate * 100) / 100.0
      ));

      current = endOfWeek;
    }

    return weeklyStats;
  }

  // 월별 데이터 처리
  private List<EmployeeTrendDto> getMonthlyStats(Instant from, Instant to) {
    List<EmployeeTrendDto> monthlyStats = new ArrayList<>();
    ZonedDateTime current = ZonedDateTime.ofInstant(from, ZoneId.of("UTC"));
    ZonedDateTime end = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));

    // 월의 첫날로 조정
    current = current.withDayOfMonth(1);

    while (current.isBefore(end) || current.isEqual(end)) {
      ZonedDateTime nextMonth = current.plusMonths(1);

      long currentCount = employeeRepository.countEmployeesByHireDateBetween(
          current.toInstant(), nextMonth.toInstant());

      // 이전 월의 데이터
      ZonedDateTime previousMonth = current.minusMonths(1);
      long previousCount = employeeRepository.countEmployeesByHireDateBetween(
          previousMonth.toInstant(), current.toInstant());

      // 변화량 계산
      long change = currentCount - previousCount;
      double changeRate = previousCount > 0 ? (double) change / previousCount * 100 : 0.0;

      String formattedDate = current.getYear() + "." + String.format("%02d", current.getMonthValue());
      formattedDate = safeDateFormat(formattedDate, current.getYear() + "." +
          String.format("%02d", current.getMonthValue()));

      monthlyStats.add(new EmployeeTrendDto(formattedDate, currentCount, change,
          Math.round(changeRate * 100) / 100.0));

      current = nextMonth;
    }

    return monthlyStats;
  }

  // 분기별 통계 조회
  private List<EmployeeTrendDto> getQuarterlyStats(Instant from, Instant to) {
    List<EmployeeTrendDto> quarterlyStats = new ArrayList<>();
    ZonedDateTime current = ZonedDateTime.ofInstant(from, ZoneId.of("UTC"));
    ZonedDateTime end = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));

    // 분기의 첫날로 조정
    int currentQuarter = (current.getMonthValue() - 1) / 3;
    current = current.withMonth(currentQuarter * 3 + 1).withDayOfMonth(1);

    while (current.isBefore(end) || current.isEqual(end)) {
      ZonedDateTime nextQuarter = current.plusMonths(3);

      long currentCount = employeeRepository.countEmployeesByHireDateBetween(
          current.toInstant(), nextQuarter.toInstant());

      // 이전 분기의 데이터
      ZonedDateTime previousQuarter = current.minusMonths(3);
      long previousCount = employeeRepository.countEmployeesByHireDateBetween(
          previousQuarter.toInstant(), current.toInstant());

      // 변화량 계산
      long change = currentCount - previousCount;
      double changeRate = previousCount > 0 ? (double) change / previousCount * 100 : 0.0;

      String isoDate = current.format(DateTimeFormatter.ISO_LOCAL_DATE); // YYYY-MM-DD 형식

      quarterlyStats.add(new EmployeeTrendDto(
          isoDate, // ISO 날짜 사용 (차트가 인식 가능)
          currentCount,
          change,
          Math.round(changeRate * 100) / 100.0
      ));

      current = nextQuarter;
    }

    return quarterlyStats;
  }


  // 연도별 통계
  private List<EmployeeTrendDto> getYearlyStats(Instant from, Instant to) {
    List<EmployeeTrendDto> yearlyStats = new ArrayList<>();
    ZonedDateTime current = ZonedDateTime.ofInstant(from, ZoneId.of("UTC"));
    ZonedDateTime end = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));

    current = current.withDayOfYear(1);

    while (current.isBefore(end) || current.isEqual(end)) {
      ZonedDateTime nextYear = current.plusYears(1);

      long currentCount = employeeRepository.countEmployeesByHireDateBetween(
          current.toInstant(), nextYear.toInstant());

      // 전년도 데이터
      ZonedDateTime previousYear = current.minusYears(1);
      long previousCount = employeeRepository.countEmployeesByHireDateBetween(
          previousYear.toInstant(), current.toInstant());

      long change = currentCount - previousCount;
      double changeRate = previousCount > 0 ? (double) change / previousCount * 100 : 0.0;

      // 연도별 통계 메서드의 반환 부분
      String formattedDate = String.valueOf(current.getYear());
      formattedDate = safeDateFormat(formattedDate, String.valueOf(current.getYear()));

      yearlyStats.add(new EmployeeTrendDto(formattedDate, currentCount, change,
          Math.round(changeRate * 100) / 100.0));
      current = nextYear;
    }

    return yearlyStats;
  }


  // 직원 수 조회
  public long getEmployeeCount(EmployeeStatus status, Instant fromDate, Instant toDate) {
    return employeeRepositoryCustom.countByStatusAndHireDateBetween(status, fromDate, toDate);
  }

  private String safeDateFormat(String dateStr, String defaultValue) {
    if (dateStr == null || dateStr.contains("NaN")) {
      return defaultValue;
    }
    return dateStr;
  }

}
