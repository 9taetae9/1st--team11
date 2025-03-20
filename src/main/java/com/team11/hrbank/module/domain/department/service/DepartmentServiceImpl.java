package com.team11.hrbank.module.domain.department.service;

import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.department.Department;
import com.team11.hrbank.module.domain.department.dto.CursorPageResponseDepartmentDto;
import com.team11.hrbank.module.domain.department.dto.DepartmentCreateRequest;
import com.team11.hrbank.module.domain.department.dto.DepartmentDto;
import com.team11.hrbank.module.domain.department.dto.DepartmentUpdateRequest;
import com.team11.hrbank.module.domain.department.mapper.DepartmentMapper;
import com.team11.hrbank.module.domain.department.repository.DepartmentRepository;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepository;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

  private final DepartmentRepository departmentRepository;
  private final DepartmentMapper departmentMapper;
  private final EmployeeRepository employeeRepository;

  /*
   * 부서 생성
   * */
  @Override
  @Transactional
  public DepartmentDto createDepartment(DepartmentCreateRequest request) {
    // 이름 중복 검사
    if (departmentRepository.existsByName(request.getName())) {
      throw new IllegalArgumentException(
          "Department already exists with name: " + request.getName());
    }

    Department department = departmentMapper.toDepartment(request);

    // BaseEntity에서 상속받은 createdAt 필드에 대한 setter 메서드에 대한 사항
    department.setCreatedAt(Instant.now());

    Department savedDepartment = departmentRepository.save(department);

    return departmentMapper.toDepartmentDto(savedDepartment);
  }

  /*
   * 부서 수정
   * */
  @Override
  @Transactional
  public DepartmentDto updateDepartment(Long id, @Valid DepartmentUpdateRequest request) {
    Department department = departmentRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Department not found: " + id));

    if (request.getName() != null && !request.getName().equals(department.getName())) {
      if (departmentRepository.existsByName(request.getName())) {
        throw new IllegalArgumentException(
            "Department already exists with name: " + request.getName());
      }
    }

    department = departmentMapper.updateDepartmentFromRequest(department, request);

    Department updatedDepartment = departmentRepository.save(department);
    return departmentMapper.toDepartmentDto(updatedDepartment);
  }

  /*
   * 부서 삭제
   * */
  @Override
  public void deleteDepartment(Long id) {
    Department department = departmentRepository.findById(id)
        .orElseThrow(() -> ResourceNotFoundException.of("Department", "id", id));

    long employeeCount = employeeRepository.countByDepartmentId(id);
    if (employeeCount > 0) {
      throw new IllegalArgumentException("소속 직원이 있는 부서는 삭제할 수 없습니다. 소속 직원 수: " + employeeCount);
    }

    departmentRepository.delete(department);
  }

  /*
  개별 상세 조회
  */
  @Override
  public DepartmentDto getDepartmentById(Long id) {
    Department department = departmentRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("부서를 찾을 수 없습니다: " + id));

    DepartmentDto departmentDto = departmentMapper.toDepartmentDto(department);
    departmentDto.setEmployeeCount(employeeRepository.countByDepartmentId(id));

    return departmentDto;
  }


  /*
   * 부서 전체 조회 및 페이지네이션
   * */
  @Override
  @Transactional(readOnly = true)
  public CursorPageResponseDepartmentDto getAllDepartments(
      String nameOrDescription,
      Long idAfter,
      String cursor,
      Integer size,
      String sortField,
      String sortDirection) {

    // 커서 디코딩
    if (cursor != null && !cursor.isEmpty() && idAfter == null) {
      try {
        String decodedCursor = new String(Base64.getDecoder().decode(cursor),
            StandardCharsets.UTF_8);
        if (decodedCursor.contains("\"id\":")) {
          String idStr = decodedCursor.split("\"id\":")[1].split("}")[0].trim();
          idAfter = Long.parseLong(idStr);
        }
      } catch (Exception e) {
        throw new IllegalArgumentException("유효하지 않은 커서 형식입니다");
      }
    }

    // 기본값 설정
    if (size == null || size <= 0) {
      size = 10; // 기본 페이지 크기
    }

    if (sortField == null || sortField.isEmpty()) {
      sortField = "establishedDate"; // API 명세에 맞게 기본 정렬 필드를 establishedDate로 수정
    }

    boolean isAscending = sortDirection == null || "asc".equalsIgnoreCase(sortDirection);

    Pageable pageable = PageRequest.of(0, size,
        isAscending ? Sort.Direction.ASC : Sort.Direction.DESC,
        sortField);

    Page<Department> departmentPage;

    // 입력받은 검색어가 있는 경우
    if (nameOrDescription != null && !nameOrDescription.isEmpty()) {
      // idafter가 제공되었다면
      if (idAfter != null) {
        if (isAscending) { //오름차순
          departmentPage = departmentRepository.searchWithCursorAsc(
              idAfter, nameOrDescription, pageable);
        } else { //내림차순
          departmentPage = departmentRepository.searchWithCursorDesc(
              idAfter, nameOrDescription, pageable);
        }
      } else { // idafter가 제공되지 않은 경우
        departmentPage = departmentRepository.searchByNameOrDescription(
            nameOrDescription, pageable);
      }
    } else { //입력받은 검색어가 없는 경우 모든 부서 조회
      // 모든 부서 가져오기
      if (idAfter != null) {
        if (isAscending) {
          departmentPage = departmentRepository.findAllWithCursorAsc(idAfter, pageable);
        } else {
          departmentPage = departmentRepository.findAllWithCursorDesc(idAfter, pageable);
        }
      } else {
        departmentPage = departmentRepository.findAll(pageable);
      }
    }

    // 엔티티를 DTO로 변환
    List<DepartmentDto> departmentDtos = departmentPage.getContent().stream()
        .map(department -> {
          DepartmentDto dto = departmentMapper.toDepartmentDto(department);
          // 직원 수는 EmployeeRepository가 구현되면 활성화
          // dto.setEmployeeCount(employeeRepository.countByDepartmentId(department.getId()));
          return dto;
        })
        .collect(Collectors.toList());

    // 응답 생성
    CursorPageResponseDepartmentDto response = new CursorPageResponseDepartmentDto();
    response.setContent(departmentDtos);
    response.setSize(size);
    response.setTotalElements(departmentPage.getTotalElements());
    response.setHasNext(departmentPage.hasNext());

    // 더 많은 페이지가 있는 경우 커서 정보 설정
    if (!departmentDtos.isEmpty() && departmentPage.hasNext()) {
      Long lastId = departmentDtos.get(departmentDtos.size() - 1).getId();
      response.setNextIdAfter(lastId);

      // 커서 인코딩
      String nextCursor = Base64.getEncoder().encodeToString(
          String.format("{\"id\":%d}", lastId).getBytes(StandardCharsets.UTF_8));
      response.setNextCursor(nextCursor);
    }

    return response;
  }
}