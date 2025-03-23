package com.team11.hrbank.module.domain.changelog.service;

import com.team11.hrbank.module.common.dto.CursorPageResponse;
import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.changelog.ChangeLog;
import com.team11.hrbank.module.domain.changelog.HistoryType;
import com.team11.hrbank.module.domain.changelog.dto.ChangeLogDto;
import com.team11.hrbank.module.domain.changelog.dto.DiffDto;
import com.team11.hrbank.module.domain.changelog.mapper.ChangeLogMapper;
import com.team11.hrbank.module.domain.changelog.mapper.DiffMapper;
import com.team11.hrbank.module.domain.changelog.repository.ChangeLogRepository;
import com.team11.hrbank.module.domain.changelog.repository.ChangeLogSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChangeLogServiceImpl implements ChangeLogService{

  private final ChangeLogRepository changeLogRepository;
  private final ChangeLogMapper changeLogMapper;
  private final DiffMapper diffMapper;


  public CursorPageResponse<ChangeLogDto> getAllChangeLogs(String employeeNumber, HistoryType type, String memo, InetAddress ipAddress, Instant atFrom, Instant atTo, Long idAfter,
      String cursor, int size, String sortField, String sortDirection) {

    //커서 디코딩
    if (cursor != null && !cursor.isEmpty() && idAfter == null) {
      String decoded = new String(Base64.getDecoder().decode(cursor));
      idAfter = Long.parseLong(decoded.replace("{\"id\":", "").replace("}", ""));
    }

    //정렬 필드 유효성 검사
    if (!isValidSortField(sortField)) {
      throw new IllegalArgumentException("Invalid sort field: " + sortField);
    }

    String dbField = convertToDbField(sortField);

    //정렬 방향 설정
    Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
        ? Direction.DESC : Direction.ASC;

    //페이징 및 정렬 설정
    PageRequest pageRequest = PageRequest.of(0, size+1, Sort.by(direction, dbField));

    Instant cursorAt = null;
    String cursorIpAddress = null;

    if(cursor != null && !cursor.isEmpty()){
      if("at".equals(sortField)){
        try{
          //at 필드 기준 커서인 경우
          cursorAt = Instant.parse(cursor);
        }catch (Exception e){
          //파싱 실패시 idAfter 기반으로 동작
        }
      }else if("ipAddress".equals(sortField)){
        //ip 주소 기반 커서인 경우 (프로토타입에서는 id 값을 커서로 사용하는 것으로 보임)
        try {
          idAfter = Long.parseLong(cursor);
        }catch (NumberFormatException e){
          //변환 실패시 무시
        }
      }
    }


    // Specification 사용 - 커서 조건 포함
    Specification<ChangeLog> spec = ChangeLogSpecification.withFilters(
            employeeNumber, type, memo, ipAddress, atFrom, atTo, idAfter,
            cursorAt, cursorIpAddress, sortField, sortDirection);

    Page<ChangeLog> page = changeLogRepository.findAll(spec, pageRequest);
    List<ChangeLog> content = page.getContent();

    boolean hasNext = content.size() > size;

    //다음 페이지 있으면 마지막 페이지 제거
    if (hasNext) {
      content.remove(size);
    }

    List<ChangeLogDto> dtoList = changeLogMapper.toDtoList(content);

    //커서 값과 nextIdAfter 설정
    String nextCursor = null;
    Long nextIdAfter = null;

    if (!content.isEmpty()) {
      ChangeLog lastItem = content.get(content.size() - 1);
      nextIdAfter = lastItem.getId();

      //정렬 필드에 따라 커서 값 설정
      if("at".equals(sortField)){
        nextCursor = lastItem.getCreatedAt().toString();
      }else if("ipAddress".equals(sortField)){
        // TODO
        //프로토타입 기준 id를 커서로 적용? (일단 ip를 커서로 적용 후 오류 발생시 프로토타입 대로 가기)
        nextCursor = lastItem.getIpAddress().toString();
      }
    }

    long totalElements = getChangeLogsCount(atFrom, atTo);
    return CursorPageResponse.of(
        dtoList,
        nextCursor,
        nextIdAfter,
        size,
        totalElements
    );
  }

  public List<DiffDto> getChangeLogDiffs(Long id) {

    ChangeLog changeLog = changeLogRepository.findById(id)
        .orElseThrow(() -> ResourceNotFoundException.of("ChangeLog", "id", id));

    if (changeLog.getChangeLogDiff() == null) {
      return List.of();
    }

    return diffMapper.toDtoList(changeLog.getChangeLogDiff().getChanges());
  }

  public long getChangeLogsCount(Instant fromDate, Instant toDate) {
    //시작 일시 (기본값: 7일 전)
    Instant from = fromDate != null ? fromDate : Instant.now().minus(7, ChronoUnit.DAYS);
    Instant to = toDate != null ? toDate : Instant.now();

    //기간 유효성 검증
    if (from.isAfter(to)) {
      throw new IllegalArgumentException("fromDate must be before toDate");
    }

    if (fromDate != null && toDate != null) {
      return changeLogRepository.countByDateRangeBoth(from, to);
    } else if (fromDate != null) {
      return changeLogRepository.countByDateRangeFrom(from);
    } else if (toDate != null) {
      return changeLogRepository.countByDateRangeTo(to);
    } else {
      return changeLogRepository.countAll();
    }
  }

  private boolean isValidSortField(String field) {
    return field != null && (field.equals("ipAddress") || field.equals("at"));
  }

  private String convertToDbField(String sortField) {
    if ("at".equals(sortField)) {
      return "createdAt";
    }

    return sortField;
  }
}
