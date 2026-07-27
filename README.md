# Last Mission

사내 인증·인증서·데이터베이스 인프라와 연동하는 프런트엔드·백엔드 모노레포입니다.

## 구성

- `frontend`: Next.js, Redux Toolkit, RTK Query, STOMP 메신저
- `backend`: Spring Boot, JDBC, WebSocket, CockroachDB, Kafka, Vault 인증서 연동

현재 사용자 화면은 로그인 사용자 정보와 메신저를 제공하며, 관리자 회원 관리 화면은 `업데이트 예정입니다.`라는 자리표시만 표시합니다. 시험 관련 기능은 포함하지 않습니다.

## 명명 규칙

- 서비스·저장소 이름: `last-mission`
- 기술 네임스페이스 및 데이터베이스 후보명: `lastmission`
- Java 기본 패키지: `com.coderhan.lastmission`
- 인증서 코어 프로젝트 식별자: `lastmission`

Git 저장소 초기화와 사내 인프라 리소스 생성은 설정 검증이 끝난 뒤 진행합니다.
