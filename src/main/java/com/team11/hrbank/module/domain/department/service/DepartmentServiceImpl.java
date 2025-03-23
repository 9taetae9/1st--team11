package com.team11.hrbank.module.domain.department.service;

import com.team11.hrbank.module.common.dto.CursorPageResponse;
import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.department.Department;
import com.team11.hrbank.module.domain.department.dto.DepartmentCreateRequest;
import com.team11.hrbank.module.domain.department.dto.DepartmentDto;
import com.team11.hrbank.module.domain.department.dto.DepartmentUpdateRequest;
import com.team11.hrbank.module.domain.department.mapper.DepartmentMapper;
import com.team11.hrbank.module.domain.department.repository.DepartmentRepository;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;


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
        // 이름 중복 검사 (띄어쓰기 제거 후 비교)
        String normalizedNewName = request.name().replace(" ", "");
        List<Department> allDepartments = departmentRepository.findAll();

        boolean duplicateExists = allDepartments.stream()
                .anyMatch(d -> d.getName().replace(" ", "").equalsIgnoreCase(normalizedNewName));

        if (duplicateExists) {
            throw new IllegalArgumentException(
                    "같은 이름의 부서가 이미 존재합니다: " + request.name());
        }

        Department department = departmentMapper.toDepartment(request);
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

        // 이름이 변경되었을 때, 띄어쓰기 제거 후 중복 검사
        if (request.name() != null && !request.name().equals(department.getName())) {
            String normalizedNewName = request.name().replace(" ", "");

            List<Department> allDepartments = departmentRepository.findAll();
            boolean duplicateExists = allDepartments.stream()
                    .filter(d -> !d.getId().equals(id)) // 자기 자신은 제외
                    .anyMatch(d -> d.getName().replace(" ", "").equalsIgnoreCase(normalizedNewName));

            if (duplicateExists) {
                throw new IllegalArgumentException(
                        "같은 이름의 부서가 이미 존재합니다 : " + request.name());
            }
        }

        department = departmentMapper.updateDepartmentFromRequest(department, request);
        Department updatedDepartment = departmentRepository.save(department);

        Long employeeCount = employeeRepository.countByDepartmentId(department.getId());
        return departmentMapper.toDepartmentDtoWithEmployeeCount(updatedDepartment, employeeCount);
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
                .orElseThrow(() -> ResourceNotFoundException.of("Department", "id", id));

        //직원 수 카운트 + dto 생성
        long employeeCount = employeeRepository.countByDepartmentId(id);
        return departmentMapper.toDepartmentDtoWithEmployeeCount(department, employeeCount);
    }


    /*
     * 부서 전체 조회 및 페이지네이션
     * */
    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<DepartmentDto> getAllDepartments(
            String nameOrDescription,
            Long idAfter,
            String cursor,
            int size,
            String sortField,
            String sortDirection) {


        if (sortField == null || sortField.isEmpty()) {
            sortField = "establishedDate";
        }

        boolean isAscending = sortDirection == null || "asc".equalsIgnoreCase(sortDirection);

        // 전체 항목 수 계산
        long totalDepartmentCount;
        if (nameOrDescription != null && !nameOrDescription.isEmpty()) {
            totalDepartmentCount = departmentRepository.countByNameOrDescriptionContaining(nameOrDescription);
        } else {
            totalDepartmentCount = departmentRepository.count();
        }

        // ========== 페이지 번호 및 나머지 항목 수 계산 개선 ==========
        long processedItems = 0;
        int pageNumber = 0;

        // idAfter를 기반으로 이미 처리된 항목 수 계산
        if (idAfter != null) {
            // ID 기준 단순화된 계산 방식 (더 정확한 방법으로 대체)
            pageNumber = (int)(idAfter / size);
            processedItems = pageNumber * size;
        }

        // 실제 남은 항목 수 계산
        long remainingItems = Math.max(0, totalDepartmentCount - processedItems);

        // 이번 페이지에서 가져올 항목 수 결정
        int effectiveSize = Math.max(1, Math.min(size, (int)remainingItems));

        // 추가 항목을 가져와 다음 페이지 확인
        int querySize = Math.max(1, Math.min(effectiveSize + 1, (int)remainingItems));

        // 마지막 페이지 여부 결정
        boolean isLastPage = remainingItems <= size;

        // 커서 처리 (기존 코드 활용)
        Department cursorDepartment = null;
        if (idAfter != null) {
            cursorDepartment = departmentRepository.findById(idAfter).orElse(null);
        } else if (cursor != null && !cursor.isEmpty()) {
            // 기존 커서 처리 로직
            if ("name".equals(sortField)) {
                // 이름 기반 커서 처리
                List<Department> matchingDepts = departmentRepository.findByName(cursor);
                if (!matchingDepts.isEmpty()) {
                    cursorDepartment = matchingDepts.get(0);
                    idAfter = cursorDepartment.getId();
                }
            } else if ("establishedDate".equals(sortField)) {
                // 날짜 기반 커서 처리
                try {
                    LocalDate date = LocalDate.parse(cursor);
                    List<Department> matchingDepts = departmentRepository.findByEstablishedDate(date);
                    if (!matchingDepts.isEmpty()) {
                        cursorDepartment = matchingDepts.get(0);
                        idAfter = cursorDepartment.getId();
                    }
                } catch (Exception e) {
                    // 파싱 실패 시 무시
                }
            } else {
                // ID 기반 커서 처리
                try {
                    idAfter = Long.parseLong(cursor);
                    cursorDepartment = departmentRepository.findById(idAfter).orElse(null);
                } catch (NumberFormatException e) {
                    // 파싱 실패 시 무시
                }
            }
        }

        // 정렬 및 페이징 설정
        Pageable pageable = PageRequest.of(0, querySize,
                Sort.by(isAscending ? Sort.Direction.ASC : Sort.Direction.DESC, sortField)
                        .and(Sort.by(Sort.Direction.ASC, "name")));

        // 데이터 조회
        Page<Department> departmentPage;
        if (nameOrDescription != null && !nameOrDescription.isEmpty()) {
            if (idAfter != null) {
                if (isAscending) {
                    departmentPage = departmentRepository.searchWithCursorAsc(
                            idAfter, nameOrDescription, pageable);
                } else {
                    departmentPage = departmentRepository.searchWithCursorDesc(
                            idAfter, nameOrDescription, pageable);
                }
            } else {
                // 커서 없이 일반 검색 메소드 사용
                departmentPage = departmentRepository.searchByNameOrDescription(
                        nameOrDescription, pageable);
            }
        } else {
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

// 결과 처리
        List<Department> departments = new ArrayList<>(departmentPage.getContent());

// 존재하지 않는 요소를 제거하려고 시도하지 않도록 함
        boolean hasNext = departments.size() > effectiveSize;
        if (hasNext && departments.size() > effectiveSize) {
            departments.remove(departments.size() - 1); // 마지막 요소 제거
        }

        // 빈 결과 처리
        if (departments.isEmpty()) {
            return CursorPageResponse.of(
                    List.of(),
                    null,
                    null,
                    effectiveSize,
                    totalDepartmentCount,
                    false
            );
        }

        // 메모리 내 정렬 (필요한 경우)
        sortDepartments(departments, sortField, isAscending);

        // DTO 변환
        List<DepartmentDto> departmentDtos = departments.stream()
                .map(department -> {
                    Long employeeCount = employeeRepository.countByDepartmentId(department.getId());
                    return departmentMapper.toDepartmentDtoWithEmployeeCount(department, employeeCount);
                })
                .toList();

        // 다음 페이지 커서 설정
        String nextCursorValue = null;
        Long nextIdAfter = null;

        if (!departments.isEmpty() && hasNext) {
            DepartmentDto lastItem = departmentDtos.get(departmentDtos.size() - 1);
            nextIdAfter = lastItem.id();

            // 정렬 필드에 따라 커서 값 설정
            if ("name".equals(sortField)) {
                nextCursorValue = lastItem.name();
            } else if ("establishedDate".equals(sortField)) {
                nextCursorValue = lastItem.establishedDate().toString();
            } else {
                nextCursorValue = nextIdAfter.toString();
            }
        }

        // 응답 생성
        return CursorPageResponse.of(
                departmentDtos,
                hasNext ? nextCursorValue : null,
                hasNext ? nextIdAfter : null,
                departments.size(), // 실제 반환하는 항목 수
                totalDepartmentCount,
                hasNext
        );
    }

  private void sortDepartments(List<Department> departments, String sortField, boolean isAscending) {
    // 부서명에서 띄어쓰기 제거하고 비교하는 정렬 기준 (항상 오름차순)
    Comparator<Department> nameComparator = (d1, d2) -> {
      String name1 = d1.getName().replace(" ", "");
      String name2 = d2.getName().replace(" ", "");
      return name1.compareToIgnoreCase(name2);
    };

    // 부서명으로 정렬할 때
    if ("name".equals(sortField)) {
      // 오름차순/내림차순에 따라 정렬
      if (isAscending) {
        departments.sort(nameComparator); //오름
      } else {
        departments.sort(nameComparator.reversed()); //내림
      }
    }
    // 설립일로 정렬할 때
    else if ("establishedDate".equals(sortField)) {
      Comparator<Department> dateAndNameComparator = (d1, d2) -> {
        int dateCompare;
        if (isAscending) {
          dateCompare = d1.getEstablishedDate().compareTo(d2.getEstablishedDate());
        } else {
          dateCompare = d2.getEstablishedDate().compareTo(d1.getEstablishedDate());
        }

        if (dateCompare != 0) {
          return dateCompare;
        }

        // 설립일이 같으면 이름 기준으로 한 번 더 정렬 (띄어쓰기 제거해서 비교)
        return nameComparator.compare(d1, d2);
      };

      departments.sort(dateAndNameComparator);
    }
    // 그 외 다른 필드로 정렬할 때도 이름 기준 정렬 한 번 더 적용해줌
    else {
      departments.sort(nameComparator);
    }
  }
}