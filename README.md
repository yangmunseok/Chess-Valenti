# Chess Valenti (체스 발렌티)

Chess Valenti는 체스 게임 분석, 데이터 시각화 및 커뮤니티 기능을 제공하는 통합 체스 플랫폼입니다. 사용자는 자신의 체스 게임 기록을 분석하고, 특정 기물 배치나 폰
구조를 기반으로 게임을 검색하며, Lichess API를 통해 통계 데이터를 제공받을 수 있습니다.

## 🚀 주요 기능

### 1. 체스 게임 분석 및 검색

https://github.com/user-attachments/assets/2768227f-eb02-46d3-8896-dae46499edae

https://github.com/user-attachments/assets/adad90f8-155b-4059-91a1-065741d9d69d

https://github.com/user-attachments/assets/291a835d-5e8a-4d66-b9e8-88bce533fee8

- **폰 구조 기반 검색:** 특정 FEN(Forsyth-Edwards Notation)을 입력하여 유사한 폰 구조를 가진 게임을 검색합니다.
- **기물 구성 필터링:** 퀸, 룩, 비숍, 나이트의 개수를 지정하여 정교하게 게임을 필터링할 수 있습니다.
- **실시간 보드 렌더링:** 검색된 게임을 웹 보드에서 즉시 확인하고 수순을 따라가며 분석할 수 있습니다.

### 2. Chess Insight (데이터 시각화)

- **플랫폼 연동:** Lichess 및 Chess.com API를 통해 사용자의 게임 데이터를 동기화합니다.
- **통계 분석:** 폰구조별 게임 수, 색깔별 승률등 다양한 통계 데이터를 시각화하여 제공합니다.
-

### 3. 커뮤니티 및 소통

- **계층형 댓글 시스템:** 공지사항, 스터디 게시글에 댓글 및 대댓글을 달아 사용자 간 소통이 가능합니다.
- **1:1 문의 및 답변:** 카테고리별 문의 기능을 제공하며, 운영진은 전용 에디터를 통해 답변을 남길 수 있습니다. 유저는 자신의 문의 내역에서 답변 상태를 실시간으로
  확인합니다.
- **공지사항 및 FAQ:** 서비스 소식과 자주 묻는 질문을 체계적으로 관리합니다.

### 4. 사용자 편의 및 보안

- **비밀번호 찾기:** 가입된 이메일로 비밀번호 재설정 링크를 발송하며, 보안 토큰 방식을 통해 안전하게 비밀번호를 변경할 수 있습니다.

### 5. 관리자 도구 (Admin Dashboard)

https://github.com/user-attachments/assets/d6fd8c3d-1a22-4bee-9d83-2a9f82a47b1b

https://github.com/user-attachments/assets/564c30a7-1101-4a59-b1c7-7eeb5a4830cc

- **통합 콘텐츠 관리:** 게시글(공지사항, 스터디, FAQ) 및 1:1 문의를 리스트 형태로 관리하고 즉시 삭제 및 처리할 수 있습니다.
- **사용자 관리:** 전체 사용자 목록 조회, 권한(Role) 변경 및 차단(Ban) 기능.
- **지표 모니터링:** 신규 가입자 추이, 실시간 온라인 사용자, 멤버십 전환율 등 주요 서비스 지표를 시각화합니다.

## 🛠 기술 스택

### Backend

- **Spring Boot**
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

- **MySQL** (dev 환경)
- **PostgreSQL** (배포 환경)

## 📂 핵심 프로젝트 구조 (Core Classes)

### 1. 체스 엔진 및 검색 로직 (Engine & Search)

* **`GameIndex` (Domain):** 복잡한 체스 판의 상태를 수학적인 숫자(Bitboard)로 변환하여 저장합니다. 이를 통해 수백만 개의 게임 중 내가 원하는 '폰
  배치'를 순식간에 찾아낼 수 있는 설계도 역할을 합니다.
* **`GameService` (Service):** 프로젝트의 '사서'와 같습니다. 사용자가 입력한 특정 기물 구성이나 폰 구조를 바탕으로 데이터베이스에서 가장 유사한 실제
  대국들을 빠르게 찾아와 사용자에게 전달합니다.
* **`Chesslib` (External Library):** 체스의 모든 규칙(기물의 이동, 체크메이트 등)을 알고 있는 두뇌 역할을 하며, PGN 형식의 기보 데이터를
  프로그램이 이해할 수 있게 해독합니다.

### 2. 데이터 분석 및 통계 (Insight & Analysis)

* **`InsightService` (Service):** 사용자의 '개인 분석가'입니다. Lichess나 Chess.com 같은 외부 사이트에서 대국 기록을 가져와 승률,
  선호하는 배치 등 통계 데이터를 계산하고 시각화하기 위한 데이터를 가공합니다.
* **`LichessApi` & `ChessComApi` (Service/API):** 외부 체스 플랫폼과 소통하는 API입니다. 각 플랫폼의 서로 다른 데이터 형식을
  표준화하여 우리 시스템으로 안전하게 가져오는 역할을 합니다.
* **`JobService` (Service):** 수많은 대국 데이터를 분석할 때 서버가 멈추지 않도록 백그라운드에서 작업을 관리합니다.

### 3. 사용자 경험 및 커뮤니티 (UX & Community)

* **`PostService` & `InquiryService` (Service):** 사용자들이 정보를 공유하는 게시판과 1:1로 소통하는 고객센터의 심장부입니다. 댓글의 계층
  구조나 에디터의 리치 텍스트 처리를 담당합니다.

### 4. 실시간 통신 및 웹 (WebSocket & Web)

* **`WebSocketConfig` (Config):** 분석 결과나 알림이 새로고침 없이 사용자 화면에 즉시 나타날 수 있도록 서버와 브라우저 사이에 실시간 통로를
  열어줍니다.
* **`GlobalExceptionHandler` (Controller Advice):** 프로그램 실행 중 발생하는 예기치 못한 에러들을 감지하고, 사용자에게 친절한 안내
  문구를 보여주어 서비스의 안정성을 높입니다.

## 📄 라이선스

이 프로젝트는 개인 연습 및 포트폴리오 목적으로 제작되었습니다.
