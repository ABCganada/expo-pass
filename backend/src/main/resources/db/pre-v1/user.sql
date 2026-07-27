-- 사용자 / 역할 스키마 (user_ 도메인 접두어)
--
-- 중앙 인증(auth-core)으로 로그인한 사용자를 이 서비스 DB에 프로비저닝한다.
--   user_accounts   : 로그인 사용자 (auth_uuid = 중앙 인증 subject)
--   user_role_codes : 역할 코드 마스터 (ADMIN/MANAGER/USER/DEVELOPER)
--   user_roles      : 사용자↔역할 매핑
--
-- 테이블·컬럼 이름은 애플리케이션 SQL(JdbcUserAccountRepository / JdbcUserRoleRepository /
-- JdbcUserAccountLock / JdbcUserDirectory)이 하드코딩한 값과 반드시 일치해야 한다.
--
-- 주의: CockroachDB 는 DDL 을 비동기 처리하므로 한 문장씩 실행한다(배치로 던지면 55000).


-- ============================================================
-- 1. 사용자
-- ============================================================

CREATE TABLE IF NOT EXISTS user_accounts (
    id INT8 NOT NULL DEFAULT unique_rowid(),
    auth_uuid VARCHAR(255) NOT NULL,          -- 중앙 인증 subject (고유)
    email VARCHAR(320) NOT NULL DEFAULT '',
    name VARCHAR(120) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_accounts_pkey PRIMARY KEY (id),
    CONSTRAINT user_accounts_auth_uuid_key UNIQUE (auth_uuid),
    CONSTRAINT user_accounts_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);


-- ============================================================
-- 2. 역할 코드 마스터
-- ============================================================

CREATE TABLE IF NOT EXISTS user_role_codes (
    id INT8 NOT NULL DEFAULT unique_rowid(),
    code VARCHAR(30) NOT NULL,
    CONSTRAINT user_role_codes_pkey PRIMARY KEY (id),
    CONSTRAINT user_role_codes_code_key UNIQUE (code),
    CONSTRAINT user_role_codes_code_check CHECK (code IN ('ADMIN', 'MANAGER', 'USER', 'DEVELOPER'))
);


-- ============================================================
-- 3. 사용자↔역할
-- ============================================================

CREATE TABLE IF NOT EXISTS user_roles (
    user_id INT8 NOT NULL,
    role_id INT8 NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id),
    CONSTRAINT user_roles_user_fkey FOREIGN KEY (user_id) REFERENCES user_accounts (id) ON DELETE CASCADE,
    CONSTRAINT user_roles_role_fkey FOREIGN KEY (role_id) REFERENCES user_role_codes (id)
);


-- ============================================================
-- 4. 역할 시드 (로그인 시 기본 USER 역할 부여에 필요)
-- ============================================================

INSERT INTO user_role_codes (code) VALUES ('ADMIN'), ('MANAGER'), ('USER'), ('DEVELOPER')
    ON CONFLICT (code) DO NOTHING;


-- ============================================================
-- 5. 권한 (애플리케이션 DB 유저)
-- ============================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE user_accounts TO lastmission_svc_main;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE user_roles TO lastmission_svc_main;
GRANT SELECT ON TABLE user_role_codes TO lastmission_svc_main;
