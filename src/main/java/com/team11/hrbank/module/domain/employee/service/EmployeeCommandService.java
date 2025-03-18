package com.team11.hrbank.module.domain.employee.service;

import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.EmployeeNumberGenerator;
import com.team11.hrbank.module.domain.changelog.ChangeLog;
import com.team11.hrbank.module.domain.changelog.ChangeLogDiff;
import com.team11.hrbank.module.domain.changelog.DiffEntry;
import com.team11.hrbank.module.domain.changelog.HistoryType;
import com.team11.hrbank.module.domain.department.repository.DepartmentRepository;
import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import com.team11.hrbank.module.domain.employee.dto.EmployeeCreateRequest;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeUpdateRequest;
import com.team11.hrbank.module.domain.employee.mapper.EmployeeMapper;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepository;
import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.service.FileService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EmployeeCommandService {

  private final EmployeeRepository employeeRepository;
  private final FileService fileService;
  //
  private final DepartmentRepository departmentRepository;

  //
  private final EmployeeMapper employeeMapper;
  //
  private final EmployeeNumberGenerator employeeNumberGenerator;
//  private final FileRepository fileRepository;

  private static final Logger log = LoggerFactory.getLogger(EmployeeCommandService.class);

  // 직원 생성
  @Transactional
  public EmployeeDto createEmployee(EmployeeCreateRequest employeeCreateRequest,
      MultipartFile file) throws Exception {

    File savedProfileImage = null;
    /** 요구 조건 : 이메일 중복 여부 검증 -> 레포지토리 단에서 email 컬럼만 들고올 수 있는 메서드 작성 **/
    if (
        employeeRepository.findAllEmails().stream()
            .anyMatch(email -> email.equals(employeeCreateRequest.email()))
    ) {
      throw new IllegalArgumentException("email(" + employeeCreateRequest.email() + ")은 이미 존재합니다.");
    }

    if (file != null && !file.isEmpty()) {
      savedProfileImage = fileService.uploadFile(file);
      log.info("직원 프로필 이미지 업로드 성공: {}", savedProfileImage.getFileName());
    }

    // 직원 생성
    Employee employee = Employee.builder()
        .name(employeeCreateRequest.name())
        .email(employeeCreateRequest.email())
        .employeeNumber(employeeNumberGenerator.generateEmployeeNumber())
        .department(departmentRepository.findById(
            employeeCreateRequest.departmentId()).orElseThrow(
            () -> new ResourceNotFoundException("부서(" + employeeCreateRequest.departmentId()
                + ")가 존재하지 않습니다.")))
        .position(employeeCreateRequest.position())
        .hireDate(employeeCreateRequest.hireDate())
        .profileImage(savedProfileImage)
        .status(EmployeeStatus.ACTIVE) // 재직중 초기화 조건, 엔티티에 설정된 에노테이션은 DB 레벨에 지정된 것
        .build();

    // 직원 저장
    employeeRepository.save(employee);

    // 직원 변경 이력 생성
    ChangeLog.create(employee,
        employee.getEmployeeNumber(),
        employeeCreateRequest.memo(),
        InetAddress.getByName("127.0.0.1"),
        HistoryType.CREATED);

    return employeeMapper.toDto(employee);
  }

  // 직원 수정
  @Transactional
  public EmployeeDto updateEmployee(Long id, EmployeeUpdateRequest employeeUpdateRequest,
      MultipartFile file) throws IOException {

    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("employee(" + id + ")는 존재하지 않습니다."));

    if (employeeUpdateRequest.name() != null) {
      // 직원 수정 이력 생성
      ChangeLog changeLog;
      try {
        changeLog = ChangeLog.create(
            employee,
            employee.getEmployeeNumber(),
            "이름 변경",
            InetAddress.getByName("127.0.0.1"),
            HistoryType.UPDATED
        );
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      }

      List<DiffEntry> changes = Arrays.asList(
          DiffEntry.of("이름", employee.getName(), employeeUpdateRequest.name()));

      ChangeLogDiff changeLogDiff = ChangeLogDiff.create(changeLog, changes);
      changeLog.setChangeLogDiff(changeLogDiff);

      // 직원 이름 업데이트
      employee.updateName(employeeUpdateRequest.name());

    }

    if (employeeUpdateRequest.email() != null) {
      /** 요구 조건 : 이메일 중복 여부 검증, 중복으로 검증 로직 분리 고려 (된다면)**/
      if (
          employeeRepository.findAllEmails().stream()
              .anyMatch(email -> email.equals(employeeUpdateRequest.email()))
      ) {
        throw new IllegalArgumentException(
            "email(" + employeeUpdateRequest.email() + ")은 이미 존재합니다.");
      }

      // 직원 수정 이력 생성
      ChangeLog changeLog;
      try {
        changeLog = ChangeLog.create(
            employee,
            employee.getEmployeeNumber(),
            "이메일 수정",
            InetAddress.getByName("127.0.0.1"),
            HistoryType.UPDATED
        );
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      }

      List<DiffEntry> changes =
          Arrays.asList(
              DiffEntry.of("이메일", employee.getEmail(), employeeUpdateRequest.email()));

      ChangeLogDiff changeLogDiff = ChangeLogDiff.create(changeLog, changes);
      changeLog.setChangeLogDiff(changeLogDiff);

      // 직원 이메일 업데이트
      employee.updateEmail(employeeUpdateRequest.email());
    }

    if (employeeUpdateRequest.departmentId() != null) {
      // 직원 수정 이력 생성
      ChangeLog changeLog;
      try {
        changeLog = ChangeLog.create(
            employee,
            employee.getEmployeeNumber(),
            "부서 수정",
            InetAddress.getByName("127.0.0.1"),
            HistoryType.UPDATED
        );
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      }

      List<DiffEntry> changes =
          Arrays.asList(
              DiffEntry.of("부서",
                  employee.getDepartment().getName(),
                  String.valueOf(
                      departmentRepository.findById(employeeUpdateRequest.departmentId())
                          .orElseThrow(
                              () -> new ResourceNotFoundException("해당 부서는 존재하지 않습니다.")))
              )
          );

      ChangeLogDiff changeLogDiff = ChangeLogDiff.create(changeLog, changes);
      changeLog.setChangeLogDiff(changeLogDiff);

      // 직원 부서 업데이트
      employee.updateDepartment(departmentRepository
          .findById(employeeUpdateRequest.departmentId())
          .orElseThrow(() -> ResourceNotFoundException.of("Department", "id",
              employeeUpdateRequest.departmentId())));
    }

    if (employeeUpdateRequest.position() != null) {

      // 직원 수정 이력 생성
      ChangeLog changeLog;
      try {
        changeLog = ChangeLog.create(
            employee,
            employee.getEmployeeNumber(),
            "직함 수정",
            InetAddress.getByName("127.0.0.1"),
            HistoryType.UPDATED
        );
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      }

      List<DiffEntry> changes =
          Arrays.asList(
              DiffEntry.of("직함", employee.getPosition(), employeeUpdateRequest.position()));

      ChangeLogDiff changeLogDiff = ChangeLogDiff.create(changeLog, changes);
      changeLog.setChangeLogDiff(changeLogDiff);

      // 직원 직함 변경
      employee.updatePosition(employeeUpdateRequest.position());
    }
    if (employeeUpdateRequest.hireDate() != null) {

      // 직원 수정 이력 생성
      ChangeLog changeLog;
      try {
        changeLog = ChangeLog.create(
            employee,
            employee.getEmployeeNumber(),
            "입사일 수정",
            InetAddress.getByName("127.0.0.1"),
            HistoryType.UPDATED
        );
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      }

      List<DiffEntry> changes =
          Arrays.asList(
              DiffEntry.of("입사일", employee.getHireDate().toString(),
                  employeeUpdateRequest.hireDate().toString()));

      ChangeLogDiff changeLogDiff = ChangeLogDiff.create(changeLog, changes);
      changeLog.setChangeLogDiff(changeLogDiff);

      // 직원 입사일 변경
      employee.updateHireDate(employeeUpdateRequest.hireDate());
    }
    if (employeeUpdateRequest.status() != null) {

      // 직원 수정 이력 생성
      ChangeLog changeLog;
      try {
        changeLog = ChangeLog.create(
            employee,
            employee.getEmployeeNumber(),
            "상태 수정",
            InetAddress.getByName("127.0.0.1"),
            HistoryType.UPDATED
        );
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      }

      List<DiffEntry> changes =
          Arrays.asList(
              DiffEntry.of("상태", employee.getStatus().toString(),
                  employeeUpdateRequest.status().toString()));

      ChangeLogDiff changeLogDiff = ChangeLogDiff.create(changeLog, changes);
      changeLog.setChangeLogDiff(changeLogDiff);

      // 직원 상태 변경
      employee.updateStatus(employeeUpdateRequest.status());
    }
    if (employeeUpdateRequest.memo() != null) {

      // 직원 수정 이력 생성 - 메모 생성
      ChangeLog changeLog;
      try {
        changeLog = ChangeLog.create(
            employee,
            employee.getEmployeeNumber(),
            "메모 생성",
            InetAddress.getByName("127.0.0.1"),
            HistoryType.CREATED
        );
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      }

      List<DiffEntry> changes =
          Arrays.asList(
              DiffEntry.of("메모 수정", "",
                  employeeUpdateRequest.memo()));

      ChangeLogDiff changeLogDiff = ChangeLogDiff.create(changeLog, changes);
      changeLog.setChangeLogDiff(changeLogDiff);

    }
    if (file != null) {
      log.info("파일 업데이트 시작 - 원본 파일명: {}, 크기: {} bytes",
          file.getOriginalFilename(), file.getSize());

      if (file.isEmpty()) {
        log.warn("업로드된 파일이 비어 있습니다. 프로필 이미지 업데이트를 건너뜁니다.");
      } else {
        // 직원 수정 이력 생성 - 파일 수정
        ChangeLog changeLog;
        try {
          changeLog = ChangeLog.create(
              employee,
              employee.getEmployeeNumber(),
              "파일 수정",
              InetAddress.getByName("127.0.0.1"),
              HistoryType.UPDATED
          );
        } catch (UnknownHostException e) {
          throw new RuntimeException(e);
        }

        // FileService를 사용하여 프로필 이미지 업데이트
        try {
          log.info("기존 프로필 이미지: {}",
              (employee.getProfileImage() != null) ?
                  employee.getProfileImage().getId() + " - " + employee.getProfileImage()
                      .getFileName() : "없음");

          // 기존 이미지가 있으면 업데이트, 없으면 새로 업로드
          File newProfileImage = fileService.updateFile(employee.getProfileImage(), file);

          log.info("새 프로필 이미지 생성됨: ID={}, 파일명={}, 경로={}",
              newProfileImage.getId(), newProfileImage.getFileName(),
              newProfileImage.getFilePath());

          employee.updateProfileImage(newProfileImage);
          log.info("직원 프로필 이미지 업데이트 성공: {}", newProfileImage.getFileName());
        } catch (IOException e) {
          log.error("프로필 이미지 업데이트 실패: {}", e.getMessage(), e);
          throw new RuntimeException("프로필 이미지 업데이트 중 오류 발생", e);
        }

        List<DiffEntry> changes =
            Arrays.asList(
                DiffEntry.of("파일 수정",
                    (employee.getProfileImage() != null
                        && employee.getProfileImage().getFileName() != null) ?
                        employee.getProfileImage().getFileName() : "없음",
                    file.getOriginalFilename() != null ? file.getOriginalFilename()
                        : file.getName()));

        ChangeLogDiff changeLogDiff = ChangeLogDiff.create(changeLog, changes);
        changeLog.setChangeLogDiff(changeLogDiff);
      }
    }

    return employeeMapper.toDto(employee);
  }

  // 직원 삭제
  @Transactional
  public void deleteEmployee(Long id) {
    // 직원 조회
    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("employee(" + id + ")는 존재하지 않습니다."));

    // 프로필 이미지가 존재하는 경우 처리
    if (employee.getProfileImage() != null && employee.getProfileImage().getId() != null) {
      // FileService를 사용하여 파일 삭제
      try {
        boolean deleted = fileService.deleteFile(employee.getProfileImage());
        if (deleted) {
          log.info("직원 프로필 이미지 삭제 성공: {}", employee.getProfileImage().getFileName());
        } else {
          log.warn("직원 프로필 이미지 삭제 실패: {}", employee.getProfileImage().getFileName());
        }
      } catch (Exception e) {
        log.error("프로필 이미지 삭제 중 오류 발생: {}", e.getMessage());
        // 이미지 삭제 실패해도 직원 삭제는 진행
      }
    }

    // 직원 상태 변경 (퇴사 처리)
    employee.updateStatus(EmployeeStatus.RESIGNED);
  }
}
