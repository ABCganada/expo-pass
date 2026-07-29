-- ============================================
-- User 도메인 테이블 변경 이력 (CockroachDB)
-- ============================================

-- [2026-07-29] MARKETER 역할 추가
-- user_role_codes CHECK 제약에 MARKETER 추가 및 행 삽입
ALTER TABLE user_role_codes DROP CONSTRAINT roles_code_check;
ALTER TABLE user_role_codes ADD CONSTRAINT roles_code_check
    CHECK (code IN ('ADMIN', 'MANAGER', 'USER', 'DEVELOPER', 'MARKETER'));
INSERT INTO user_role_codes (id, code)
    VALUES ((SELECT MAX(id) + 1 FROM user_role_codes), 'MARKETER');

-- [2026-07-29] MARKETER 역할 제거 (박람회 관리자(MANAGER)로 통합)
-- 1. MARKETER 역할을 가진 유저의 역할 행 삭제
DELETE FROM user_roles
WHERE role_id = (SELECT id FROM user_role_codes WHERE code = 'MARKETER');
-- 2. user_role_codes 에서 MARKETER 행 삭제
DELETE FROM user_role_codes WHERE code = 'MARKETER';
-- 3. CHECK 제약 갱신 (MARKETER 제외)
ALTER TABLE user_role_codes DROP CONSTRAINT roles_code_check;
ALTER TABLE user_role_codes ADD CONSTRAINT roles_code_check
    CHECK (code IN ('ADMIN', 'MANAGER', 'USER', 'DEVELOPER'));
