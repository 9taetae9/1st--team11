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

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
            Integer size,
            String sortField,
            String sortDirection) {

        // 기본값 설정
        if (size == null || size <= 0) {
            size = 10; // 기본 페이지 크기
        }

        if (sortField == null || sortField.isEmpty()) {
            sortField = "establishedDate"; // API 명세에 맞게 기본 정렬 필드를 establishedDate로 수정
        }

        boolean isAscending = sortDirection == null || "asc".equalsIgnoreCase(sortDirection);

        Department cursorDepartment = null;

        if (idAfter != null) {
            cursorDepartment = departmentRepository.findById(idAfter).orElse(null);
        } else if (cursor != null && !cursor.isEmpty()) { //커서가 필드 값인 경우
            if ("name".equals(sortField)) {
                List<Department> matchingDepts = departmentRepository.findByName(cursor);
                if (!matchingDepts.isEmpty()) {
                    //커서와 일치하는 부서 중 첫번째 항목 사용
                    cursorDepartment = matchingDepts.get(0);
                    idAfter = cursorDepartment.getId();
                }
            } else if ("establishedDate".equals(sortField)) {
                //설립일 기준 커서
                try {
                    LocalDate date = LocalDate.parse(cursor);
                    List<Department> matchingDepts = departmentRepository.findByEstablishedDate(date);
                    if (!matchingDepts.isEmpty()) {
                        //설립일 같은 부서중 선택
                        cursorDepartment = matchingDepts.get(0);
                        idAfter = cursorDepartment.getId();
                    }
                } catch (Exception e) {
                    //날짜 파싱 실패 무시
                }
            } else {
                try {
                    idAfter = Long.parseLong(cursor);
                    cursorDepartment = departmentRepository.findById(idAfter).orElse(null);
                } catch (NumberFormatException e) {
                    //id 파싱 실패 무시
                }
            }
        }


        // 부서명은 항상 오름차순으로 정렬 (설립일 내림차순일 때도)
        Pageable pageable = PageRequest.of(0, size + 1, //다음 페이지 확인 위해 size+1
                Sort.by(
                                isAscending ? Sort.Direction.ASC : Sort.Direction.DESC, sortField)
                        .and(Sort.by(Sort.Direction.ASC, "name"))
        );

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

        // 전체 부서 수 조회
        long totalDepartmentCount;
        if (nameOrDescription != null && !nameOrDescription.isEmpty()) {
            // 검색어가 있는 경우, 해당 검색어에 맞는 전체 부서 수 조회
            totalDepartmentCount = departmentRepository.countByNameOrDescriptionContaining(nameOrDescription);
        } else {
            // 검색어가 없는 경우, 전체 부서 수 조회
            totalDepartmentCount = departmentRepository.count();
        }


        // 데이터베이스에서 가져온 부서 목록
        List<Department> departments = new ArrayList<>(departmentPage.getContent());

        boolean hasNext = departments.size() > size;
        //다음 페이지 존재 여부 확인
        if (departments.size() > size) {
            departments.remove(size);
        }

      // 빈 결과 처리 추가
      if (departments.isEmpty()) {
        return CursorPageResponse.of(
                List.of(),
                null,
                null,
                size,
                totalDepartmentCount,
                false

        );
      }

        //정렬 처리
        sortDepartments(departments, sortField, isAscending);

        List<DepartmentDto> departmentDtos = departments.stream()
                .map(department -> {
                    Long employeeCount = employeeRepository.countByDepartmentId(department.getId());
                    return departmentMapper.toDepartmentDtoWithEmployeeCount(department, employeeCount);
                })
                .toList();

        String nextCursorValue = null;
        Long nextIdAfter = null;
        if (!departments.isEmpty()) {
            DepartmentDto lastItem = departmentDtos.get(departments.size() - 1);
            nextIdAfter = lastItem.id();

            //정렬 필드에 따라 커서 값 설정
            if ("name".equals(sortField)) {
                nextCursorValue = lastItem.name();
            } else if ("establishedDate".equals(sortField)) {
                nextCursorValue = lastItem.establishedDate().toString();
            } else {
                nextCursorValue = nextIdAfter.toString();
            }
        }

        return CursorPageResponse.of(
                departmentDtos,
                hasNext ? nextCursorValue : null,
                hasNext ? nextIdAfter : null,
                size,
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