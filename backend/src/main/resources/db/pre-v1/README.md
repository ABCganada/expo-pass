# pre-v1 데이터베이스 스키마

Flyway 기준선 확정 전, 현재 운영 중인 스키마를 수동 적용용으로 보관한다.

- `user.sql` — 사용자 도메인: `user_accounts`, `user_role_codes`, `user_roles`
- `chat.sql` — 채팅 도메인: `chat_rooms`, `chat_participants`, `chat_messages`, `chat_reads`

## 적용 순서

`user.sql` 을 먼저 적용한다(채팅 테이블이 `user_accounts` 를 FK 로 참조하므로).
그다음 `chat.sql` 을 적용한다.

CockroachDB 는 DDL 을 비동기 처리하므로 한 문장씩 실행한다(배치로 던지면 55000).

최초 Flyway 기준선은 이 두 파일을 합쳐 새 데이터베이스 프로비저닝 시 확정한다.
