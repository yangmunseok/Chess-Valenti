# Chess Valenti (체스 발렌티)

Chess Valenti는 체스 게임 분석, 데이터 시각화 및 커뮤니티 기능을 제공하는 통합 체스 플랫폼입니다. 사용자는 자신의 체스 게임 기록을 분석하고, 특정 기물 배치나 폰
구조를 기반으로 게임을 검색하며, Lichess API를 통해 통계 데이터를 제공받을 수 있습니다.

## 🚀 주요 기능

### 1. 체스 게임 분석 및 검색

https://github.com/user-attachments/assets/2768227f-eb02-46d3-8896-dae46499edae

- **폰 구조 기반 검색:** 특정 FEN(Forsyth-Edwards Notation)을 입력하여 유사한 폰 구조를 가진 게임을 검색합니다.
- **기물 구성 필터링:** 퀸, 룩, 비숍, 나이트의 개수를 지정하여 정교하게 게임을 필터링할 수 있습니다.
- **실시간 보드 렌더링:** 검색된 게임을 웹 보드에서 즉시 확인하고 수순을 따라가며 분석할 수 있습니다.

### 2. Chess Insight (데이터 시각화)

- **플랫폼 연동:** Lichess 및 Chess.com API를 통해 사용자의 게임 데이터를 동기화합니다.
- **통계 분석:** 승률, 오프닝 선호도, 시간대별 성적 등 다양한 통계 데이터를 시각화하여 제공합니다.
- **유사 게임 필터링:** 자신과 유사한 실력대의 플레이어들이 둔 게임을 분석하여 학습 기회를 제공합니다.

### 3. 커뮤니티 및 소통

- **계층형 댓글 시스템:** 공지사항, 스터디 게시글에 댓글 및 대댓글을 달아 사용자 간 소통이 가능합니다.
- **1:1 문의 및 답변:** 카테고리별 문의 기능을 제공하며, 운영진은 전용 에디터를 통해 답변을 남길 수 있습니다. 유저는 자신의 문의 내역에서 답변 상태를 실시간으로 확인합니다.
- **공지사항 및 FAQ:** 서비스 소식과 자주 묻는 질문을 체계적으로 관리합니다.

### 4. 사용자 편의 및 보안

- **비밀번호 찾기:** 가입된 이메일로 비밀번호 재설정 링크를 발송하며, 보안 토큰 방식을 통해 안전하게 비밀번호를 변경할 수 있습니다.
- **멤버십 및 후원:** Toss Payments API를 통한 후원 시스템 및 멤버십 레벨 관리 기능을 포함합니다.

### 5. 관리자 도구 (Admin Dashboard)

https://github.com/user-attachments/assets/564c30a7-1101-4a59-b1c7-7eeb5a4830cc

https://github.com/user-attachments/assets/d6fd8c3d-1a22-4bee-9d83-2a9f82a47b1b

- **통합 콘텐츠 관리:** 게시글(공지사항, 스터디, FAQ) 및 1:1 문의를 리스트 형태로 관리하고 즉시 삭제 및 처리할 수 있습니다.
- **사용자 관리:** 전체 사용자 목록 조회, 권한(Role) 변경 및 차단(Ban) 기능.
- **지표 모니터링:** 신규 가입자 추이, 실시간 온라인 사용자, 멤버십 전환율 등 주요 서비스 지표를 시각화합니다.
- **재무 관리:** 결제 및 후원 내역의 상세 조회와 환불 요청 처리를 지원합니다.

## 🛠 기술 스택

### Backend

- **Java 17**
- **Spring Boot 3.3.5**
- **Spring Data JPA**
- **Spring Security** (OAuth2/RBAC)
- **Spring WebFlux & Reactor** (비동기 데이터 스트리밍)
- **Spring Mail** (비밀번호 재설정 및 알림)
- **Spring WebSocket** (실시간 통신)
- **Chesslib** (체스 로직 및 PGN 처리)

### Frontend

- **Thymeleaf** (템플릿 엔진)
- **Vanilla JavaScript** (ES6+, Web Workers 및 비동기 처리)
- **CSS3 / Vanilla CSS** (반응형 디자인 및 다크 테마 애니메이션)
- **Quill Editor** (게시글 및 답변 작성용 리치 텍스트 에디터)

### Database

- **MySQL**

## ⚙️ 설치 및 실행 방법

### 요구 사항

- Java 25 이상
- Maven
- MySQL

### 설정 (application.properties)

1. `src/main/resources/application.properties.example` 파일을 복사하여 `application.properties`를 생성합니다.
2. 데이터베이스 연결 정보 및 Toss Payments API 키를 설정합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/YOUR_DATABASE_NAME
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
# Toss Payment
toss.payment.secret-key=YOUR_TOSS_SECRET_KEY
```

### 실행

```bash
mvn clean install
mvn spring-boot:run
```

## 📂 프로젝트 구조

- `domain`: JPA 엔티티 클래스 (User, Payment, Post, Inquiry 등)
- `controller`: API 엔드포인트 및 페이지 컨트롤러
- `service`: 비즈니스 로직 및 외부 API(Lichess, Toss) 연동
- `dto`: 데이터 전송 객체
- `static/js/newStructure`: 모듈화된 프론트엔드 스크립트
- `templates`: Thymeleaf HTML 템플릿

## 📄 라이선스

이 프로젝트는 개인 연습 및 포트폴리오 목적으로 제작되었습니다.
