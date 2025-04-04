# 1기-HR Bank-11팀

## 👥 팀원 구성 & 역할

| 이름     | 역할                                                                 |
|----------|----------------------------------------------------------------------|
| [김태현<br>(팀장)](https://github.com/9taetae9) | - 아키텍처, db schema 설계 및 DDL 관리<br>- GitHub 관리(Branch rules, Issue Templates etc.)<br>- 직원 정보 수정 이력 시스템 구현 및 단위 테스트 작성<br>- 스케줄러 기반 자동 백업 시스템 구현<br>- 커서 기반 페이지네이션 로직 구현<br>- 개발/운영 환경 profile 구성 및 관리|
| [김경린](https://github.com/k01zero) | - 직원 도메인 모델 설계 및 비즈니스 로직 구현<br>- 커서 기반 페이지네이션 로직 구현<br>- 프로젝트 진행 일정 및 이슈 문서화(Notion)<br>- 배포 전략 수립 및 시연 |
| [박지현](https://github.com/jjhparkk) | - ERD 초기 설계<br>- 부서 도메인 모델 구현 및 정렬 기능 개선<br>- 프로젝트 진행 일정 및 이슈 문서화(Notion)<br>- 배포 전략 수립 및 시연|
| [장건희](https://github.com/Gunh2ee) | - 파일 도메인 모델 구현<br>- Swagger 문서화 |

---
## 프로젝트 소개
**Batch 기반 인적자원 관리 시스템**  
📆 프로젝트 기간: 2025.03.13 ~ 2025.03.23  

HR Bank는 직원 정보 관리, 변경 이력 추적, 부서 관리 및 데이터 백업 기능을 제공하는 종합적인 인사 관리 솔루션입니다.
본 프로젝트는 직원 데이터의 효율적인 관리와 변경 이력의 투명한 추적을 통해 인사 관리의 효율성을 높이는 것을 목적으로 합니다.

## 기능
### 부서 관리
- **정보:** 이름, 설명, 설립일  
- **기능:** 등록, 수정, 삭제(소속 직원 없을 경우), 목록 조회 (이름/설명 부분 일치, 정렬 및 페이지네이션)

### 직원 정보 관리
- **정보:** 이름, 이메일, 사원 번호(자동 부여), 부서, 직함, 입사일, 상태(자동 `재직중`), 프로필 이미지  
- **기능:** 등록, 수정(사원 번호 제외), 삭제(상태 변경), 목록 및 상세 조회

### 파일 관리
- **정보:** 파일명, 파일 형식, 크기  
- **기능:** 메타 정보(DB 저장)와 실제 파일(로컬 디스크 저장) 관리, 파일 다운로드

### 직원 정보 수정 이력 관리
- **정보:** 유형(직원 추가, 정보 수정, 직원 삭제), 대상 직원 사번, 변경 상세 내용(수정 전/후), 메모, IP 주소, 시간  
- **기능:** 이력 등록, 목록 조회(정렬 및 페이지네이션), 상세 변경 내용 조회

### 데이터 백업 관리
- **정보:** 작업자, 시작/종료 시간, 상태(`진행중`, `완료`, `실패`, `건너뜀`), 백업 파일  
- **프로세스:**  
  1. 백업 필요 여부 판단  
  2. 백업 이력 등록 (`진행중`)  
  3. 데이터 백업 실행 (CSV 파일로 저장)  
  4. 성공 시 이력 수정 (`완료`), 실패 시 로그 기록 및 파일 삭제  
- **자동 백업:** Spring Scheduler 활용 (주기 1시간, 작업자: `system`)

### 대시보드 관리
- **정보:** 총 직원 수, 최근 일주일 수정 이력 건수, 이번달 입사자 수, 마지막 백업 시간, 최근 1년 월별 직원수 변동, 부서별/직무별 직원 분포  


## 기술 스택 🛠

### 기본 기술
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![MapStruct](https://img.shields.io/badge/MapStruct-FF6F61)

### 쿼리 고도화
![Criteria API](https://img.shields.io/badge/Criteria_API-259dff)
![Specification](https://img.shields.io/badge/Specification-259dff)
![QueryDSL](https://img.shields.io/badge/QueryDSL-259dff)

### 테스트
![Postman](https://img.shields.io/badge/Postman-FF6C37?logo=Postman&logoColor=white)
![JUnit](https://img.shields.io/badge/JUnit-25A162?style=flat-square&logo=junit5&logoColor=white)
![Mockito](https://img.shields.io/badge/Mockito-D3593C?style=flat-square&logo=mockito&logoColor=white)

### API 문서화
![springdoc-openapi](https://img.shields.io/badge/springdoc--openapi-85EA2D?logo=openapiinitiative&logoColor=black)

**Swagger UI 접속 URL**: 
  - 로컬 환경: `http://localhost:8080/swagger-ui/index.html`
  - 배포 환경: [HR Bank API 문서](https://hr-bank11-production.up.railway.app/swagger-ui/index.html)

### 배포
![Railway](https://img.shields.io/badge/Railway-0B0D0E?logo=railway&logoColor=white)

---


## 프로젝트 구조
```bash
src
└── main
│   └── java
│   │   └── com
│   │   │   └── team11
│   │   │   │   └── hrbank
│   │   │   │   │   └── config
│   │   │   │   │   └── module
│   │   │   │   │   │   └── common
│   │   │   │   │   │   │   └── config
│   │   │   │   │   │   │   └── dto
│   │   │   │   │   │   │   └── exception
│   │   │   │   │   │   └── domain
│   │   │   │   │   │   │   └── backup
│   │   │   │   │   │   │   │   └── controller
│   │   │   │   │   │   │   │   └── dto
│   │   │   │   │   │   │   │   └── exception
│   │   │   │   │   │   │   │   └── mapper
│   │   │   │   │   │   │   │   └── repository
│   │   │   │   │   │   │   │   └── scheduler
│   │   │   │   │   │   │   │   └── service
│   │   │   │   │   │   │   │   │   └── data
│   │   │   │   │   │   │   │   │   └── file
│   │   │   │   │   │   │   └── changelog
│   │   │   │   │   │   │   │   └── controller
│   │   │   │   │   │   │   │   └── dto
│   │   │   │   │   │   │   │   └── mapper
│   │   │   │   │   │   │   │   └── repository
│   │   │   │   │   │   │   │   └── service
│   │   │   │   │   │   │   └── department
│   │   │   │   │   │   │   │   └── controller
│   │   │   │   │   │   │   │   └── dto
│   │   │   │   │   │   │   │   └── mapper
│   │   │   │   │   │   │   │   └── repository
│   │   │   │   │   │   │   │   └── service
│   │   │   │   │   │   │   └── employee
│   │   │   │   │   │   │   │   └── controller
│   │   │   │   │   │   │   │   └── dto
│   │   │   │   │   │   │   │   └── mapper
│   │   │   │   │   │   │   │   └── repository
│   │   │   │   │   │   │   │   └── service
│   │   │   │   │   │   │   └── file
│   │   │   │   │   │   │   │   └── controller
│   │   │   │   │   │   │   │   └── exception
│   │   │   │   │   │   │   │   └── repository
│   │   │   │   │   │   │   │   └── service
│   └── resources
│   │   └── static
│   │   │   └── assets
│   │   │   │   └── images
└── test
```


### ERD
![hrbank_erd최종](https://github.com/user-attachments/assets/b3bfd07e-b495-4b4e-8127-f7f3880166c4)




## Issue 제목
**형식**: `[타입]: 설명`

### ✍️ Commit Convention
**형식**: `[타입]: 설명 #이슈번호`  
**예시**: `feat: 설명 #12`

| 타입     | 사용 시나리오 |
|----------|---------------|
| `feat`   | 신기능 추가 |
| `fix`    | 버그 수정 |
| `refactor`| 리팩토링 |
| `chore`  | 설정 변경 |
| `docs`	 | 문서 수정에 대한 커밋 |


### 🔄 Git Flow
| 브랜치 유형 | 목적 | 병합 대상 |
|-------------|------|------------|
| `main` | 프로덕션 버전 | 직접 커밋 금지 |
| `develop` | 개발 통합 브랜치 | feature/fix 브랜치 병합 |
| `feature/#/내용` | 기능 개발 | → develop |
| `hotfix/#/내용` | 긴급 수정 | → main & develop |
