-- spring-modulith-events-jdbc-2.1.0.jar의 동일 경로 리소스를 클래스패스 우선순위로 오버라이드한다.
-- 원본 스크립트의 `CREATE INDEX ... USING hash(serialized_event)`는 CockroachDB가 지원하지 않는
-- PostgreSQL 전용 hash 인덱스 접근 방식이라("unimplemented: this syntax") 기동이 실패한다.
-- serialized_event 해시 인덱스는 조회 성능 최적화용이라 없어도 기능상 문제없어 제거했다.
CREATE TABLE IF NOT EXISTS event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
