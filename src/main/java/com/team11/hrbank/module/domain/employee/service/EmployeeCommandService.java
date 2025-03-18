package com.team11.hrbank.module.domain.employee.service;

import com.team11.hrbank.module.common.exception.ResourceNotFoundException;
import com.team11.hrbank.module.domain.EmployeeNumberGenerator;
import com.team11.hrbank.module.domain.department.repository.DepartmentRepository;
import com.team11.hrbank.module.domain.employee.Employee;
import com.team11.hrbank.module.domain.employee.EmployeeStatus;
import com.team11.hrbank.module.domain.employee.dto.EmployeeCreateRequest;
import com.team11.hrbank.module.domain.employee.dto.EmployeeDto;
import com.team11.hrbank.module.domain.employee.dto.EmployeeUpdateRequest;
import com.team11.hrbank.module.domain.employee.mapper.EmployeeMapper;
import com.team11.hrbank.module.domain.employee.repository.EmployeeRepository;
import com.team11.hrbank.module.domain.file.File;
import com.team11.hrbank.module.domain.file.dto.request.FileUploadRequest;
import com.team11.hrbank.module.domain.file.repository.FileRepository;
import com.team11.hrbank.module.domain.file.service.FileService;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
  private final FileRepository fileRepository;

  // 직원 생성
  @Transactional
  public EmployeeDto createEmployee(EmployeeCreateRequest employeeCreateRequest,
      MultipartFile file) throws Exception {

    /* TODO (#Change-log) memo 데이터를 Change-log 로 전달 -- 태현씨께 */

    /** 요구 조건 : 이메일 중복 여부 검증 -> 레포지토리 단에서 email 컬럼만 들고올 수 있는 메서드 작성 **/
    if (
        employeeRepository.findAllEmails().stream()
            .anyMatch(email -> email.equals(employeeCreateRequest.email()))
    ) {
      throw new IllegalArgumentException("email(" + employeeCreateRequest.email() + ")은 이미 존재합니다.");
    }

    // 프로필 이미지 처리 TODO : File 경로 문제 있습니다. > 지정된 경로를 찾을 수 없습니다 -- 건희씨께
    File profileImage = Optional.ofNullable(file)
        .map(FileUploadRequest::new)
        .map(fileUploadRequest -> {
          try {
            return fileService.uploadFile(fileUploadRequest);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        })
        .map(uploadedFile -> fileService.getFileById(uploadedFile.getId()))
        .orElse(null);
//    FileUploadRequest fileUploadRequest = new FileUploadRequest(file);
//    File profileImage = null;
//    if (file != null) {
//      Long fileId = fileService.uploadFile(fileUploadRequest).getId();
//      profileImage = fileService.getFileById(fileId);
//    }

    // 직원 생성
    Employee employee = Employee.builder()
        .name(employeeCreateRequest.name())
        .email(employeeCreateRequest.email())
        .employeeNumber(employeeNumberGenerator.generateEmployeeNumber())
        // TODO : 임시로 departmentRepository 지정해뒀습니다. -- 지현씨께
        .department(departmentRepository.findById(
            employeeCreateRequest.departmentId()).orElseThrow(
            () -> new ResourceNotFoundException("부서(" + employeeCreateRequest.departmentId()
                + ")가 존재하지 않습니다.")))
        .position(employeeCreateRequest.position())
        .hireDate(employeeCreateRequest.hireDate())
        .profileImage(profileImage)
        .status(EmployeeStatus.ACTIVE) // 재직중 초기화 조건, 엔티티에 설정된 에노테이션은 DB 레벨에 지정된 것
        .build();

    // 직원 저장
    employeeRepository.save(employee);

    return employeeMapper.toDto(employee);
  }

  // 직원 수정
  @Transactional
  public EmployeeDto updateEmployee(Long id, EmployeeUpdateRequest employeeUpdateRequest,
      MultipartFile file) {

    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("employee(" + id + ")는 존재하지 않습니다."));

    if (employeeUpdateRequest.name() != null) {
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
      employee.updateEmail(employeeUpdateRequest.email());
    }
    if (employeeUpdateRequest.departmentId() != null) {
      // TODO : 임시로 departmentRepository 지정해뒀습니다. -- 지현씨께
      employee.updateDepartment(departmentRepository
          .findById(employeeUpdateRequest.departmentId())
          .orElseThrow(() -> ResourceNotFoundException.of("Department", "id",
              employeeUpdateRequest.departmentId())));
    }
    if (employeeUpdateRequest.position() != null) {
      employee.updatePosition(employeeUpdateRequest.position());
    }
    if (employeeUpdateRequest.hireDate() != null) {
      employee.updateHireDate(employeeUpdateRequest.hireDate());
    }
    if (employeeUpdateRequest.status() != null) {
      employee.updateStatus(employeeUpdateRequest.status());
    }
    if (employeeUpdateRequest.memo() != null) {
      /* 이 부분 TODO (#Change-log) memo 데이터를 Change-log 로 전달 -- 태현씨께 */
    }
    if (file != null) {
      /* 이 부분 TODO : File 경로 문제 있습니다. > 지정된 경로를 찾을 수 없습니다 -- 건희씨께 */
      File profileImage = Optional.ofNullable(file)
          .map(FileUploadRequest::new)
          .map(fileUploadRequest -> {
            try {
              return fileService.uploadFile(fileUploadRequest);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          })
          .map(uploadedFile -> fileService.getFileById(uploadedFile.getId()))
          .orElse(null);
    }

    return employeeMapper.toDto(employee);
  }

  // 직원 삭제
  @Transactional
  public void deleteEmployee(Long id) {
    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("employee(" + id + ")는 존재하지 않습니다."));
    /* 이 부분 TODO (#File) 프로필 이미지(메타데이터, 실제 파일) 삭제 -- 건희씨께 */
    // TODO : 메타데이터 삭제를 어떤 방식으로 진행하면 될지 -> 토의 필요
    // TODO : 우선 실제 파일은 레포지토리로 직접 접근 -> 토의가 필요
    fileRepository.delete(employee.getProfileImage());
    employee.updateStatus(EmployeeStatus.RESIGNED);
  }
}
