# Chess Valenti (체스 발렌티)

Chess Valenti는 체스 게임 분석, 데이터 시각화 및 커뮤니티 기능을 제공하는 통합 체스 플랫폼입니다. 사용자는 자신의 체스 게임 기록을 분석하고, 특정 기물 배치나 폰
구조를 기반으로 게임을 검색하며, Lichess API를 통해 통계 데이터를 제공받을 수 있습니다.

## 🚀 주요 기능

### 1. 체스 게임 분석 및 검색

- **폰 구조 기반 검색:** 특정 FEN(Forsyth-Edwards Notation)을 입력하여 유사한 폰 구조를 가진 게임을 검색합니다.
- **기물 구성 필터링:** 퀸, 룩, 비숍, 나이트의 개수를 지정하여 정교하게 게임을 필터링할 수 있습니다.
- **실시간 보드 렌더링:** 검색된 게임을 웹 보드에서 즉시 확인하고 수순을 따라가며 분석할 수 있습니다.

### 2. Chess Insight (데이터 시각화)

- **Lichess 연동:** 사용자의 Lichess 아이디를 통해 최근 게임 데이터를 동기화합니다.
- **통계 분석:** 승률, 오프닝 선호도, 시간대별 성적 등 다양한 통계 데이터를 시각화하여 제공합니다.
- **유사 게임 필터링:** 자신과 유사한 실력대의 플레이어들이 둔 게임을 분석하여 학습 기회를 제공합니다.

### 3. 커뮤니티 및 지원

- **공지사항 및 FAQ:** 관리자가 등록한 공지사항과 자주 묻는 질문을 확인할 수 있습니다.
- **1:1 문의 시스템:** 카테고리별 문의 기능을 통해 운영진과 소통할 수 있습니다.
- **멤버십 및 후원:** Toss Payments API를 통한 후원 시스템 및 멤버십 레벨 관리 기능을 포함합니다.

### 4. 관리자 도구

- **사용자 관리:** 전체 사용자 목록 조회, 권한(Role) 변경 및 차단(Ban) 기능.
- **통계 대시보드:** 신규 가입자, 온라인 사용자 수, 멤버십 비율 등 서비스 지표 모니터링.
- **재무 관리:** 결제 및 후원 내역 리스트 조회.

## 🛠 기술 스택

### Backend

- **Java 25**
- **Spring Boot 4.0.3**
- **Spring Data JPA**
- **Spring Security** (Authentication/Authorization)
- **Spring WebFlux & Reactor** (비동기 데이터 스트리밍)
- **Spring WebSocket** (실시간 통신)
- **Chesslib** (체스 로직 및 PGN 처리)

### Frontend

- **Thymeleaf** (템플릿 엔진)
- **Vanilla JavaScript** (Web Workers 및 비동기 처리)
- **CSS3** (반응형 디자인 및 애니메이션)

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
