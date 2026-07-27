-- 채팅 (공개방 / 비공개방) — 탭 메신저.
--
-- 방은 사용자가 만들고, 만든 사람이 제목을 정한다(owner_user_id = 만든 사람, name = 그가 지은 제목).
--
--   is_public = true  → 공개방. 누구나 읽고 쓰며, 방을 실제로 열면 현재 구성원으로 기록한다(생성은 관리자만).
--   is_public = false → 비공개방. 초대된 사람만(생성은 누구나). 구성원은 chat_participants 로 관리.
--
-- 접근: 공개방 = 전원 / 비공개방 = 현재 참가자(chat_participants 에 left_at IS NULL 행이 있는 사람).
--       관리자 특례는 없다 — 관리자도 초대받은 방만 본다. "관리자 선톡"은 방을 만들어 초대하는 것.
--
-- 구성원 이력(chat_participants):
--   공개방은 사용자가 방을 실제로 열 때, 비공개방은 생성·초대할 때 참가 행을 만든다.
--   입장하면 행을 추가하고, 나가면 지우지 않고 left_at 에 시각을 채운다.
--   같은 사람이 나갔다 다시 오면 새 행을 추가한다(과거 참여 구간은 기록으로 남는다).
--   메시지 조회는 "그 참여 구간의 joined_at 이후"만 보여준다(초대 이전 대화는 안 보인다).
--   현재 참여 중(left_at IS NULL)인 행은 (room, user) 당 하나만 존재하도록 부분 유니크로 강제한다.
--
-- 경계: 발신자 표시 이름은 발신 시점 스냅샷으로 저장한다(닉네임이 바뀌어도 과거 대화는 그대로).
--
-- 주의: CockroachDB 는 DDL 을 비동기 처리하므로 한 문장씩 실행한다(배치로 던지면 55000).


-- ============================================================
-- 0. 기존 채팅 객체 제거 (데이터는 폐기 가능 — 구조 개편으로 새로 만든다)
-- ============================================================

DROP TABLE IF EXISTS chat_reads;
DROP TABLE IF EXISTS chat_participants;
DROP TABLE IF EXISTS chat_messages;
DROP TABLE IF EXISTS chat_rooms;
DROP SEQUENCE IF EXISTS chat_message_id_seq;


-- ============================================================
-- 1. 방
-- ============================================================

CREATE TABLE IF NOT EXISTS chat_rooms (
    id INT8 NOT NULL DEFAULT unique_rowid(),
    is_public BOOL NOT NULL,
    owner_user_id INT8 NOT NULL,
    name VARCHAR(60) NOT NULL,
    active BOOL NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chat_rooms_pkey PRIMARY KEY (id),
    CONSTRAINT chat_rooms_owner_fkey FOREIGN KEY (owner_user_id) REFERENCES user_accounts (id)
);

-- 방 목록을 공개/최신순으로 뽑는 조회에 맞춘다.
CREATE INDEX IF NOT EXISTS idx_chat_rooms_public ON chat_rooms (is_public, active, created_at DESC);


-- ============================================================
-- 2. 구성원 이력 (공개방 실제 입장 / 비공개방 생성·초대)
-- ============================================================

CREATE TABLE IF NOT EXISTS chat_participants (
    id INT8 NOT NULL DEFAULT unique_rowid(),
    room_id INT8 NOT NULL,
    user_id INT8 NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at TIMESTAMPTZ NULL,
    CONSTRAINT chat_participants_pkey PRIMARY KEY (id),
    CONSTRAINT chat_participants_room_fkey FOREIGN KEY (room_id)
        REFERENCES chat_rooms (id) ON DELETE CASCADE,
    CONSTRAINT chat_participants_user_fkey FOREIGN KEY (user_id) REFERENCES user_accounts (id)
);

-- 현재 참여 중인 행은 (room, user) 당 하나만. 나간 행(left_at 값 있음)은 이력으로 몇 개든 쌓인다.
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_participants_active
    ON chat_participants (room_id, user_id) WHERE left_at IS NULL;

-- "내가 속한 방" 조회용.
CREATE INDEX IF NOT EXISTS idx_chat_participants_user
    ON chat_participants (user_id, room_id);


-- ============================================================
-- 3. 메시지
-- ============================================================

-- 메시지 id 는 커서 페이징과 읽음 처리의 기준이므로 단조 증가가 필요하다.
-- unique_rowid() 는 단조 증가를 보장하지 않으므로 SEQUENCE 를 쓴다.
CREATE SEQUENCE IF NOT EXISTS chat_message_id_seq;

CREATE TABLE IF NOT EXISTS chat_messages (
    id INT8 NOT NULL DEFAULT nextval('chat_message_id_seq'),
    room_id INT8 NOT NULL,
    sender_id INT8 NOT NULL,
    sender_name VARCHAR(60) NOT NULL,
    content STRING NOT NULL,
    message_type VARCHAR(30) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT chat_messages_pkey PRIMARY KEY (id),
    CONSTRAINT chat_messages_room_id_fkey FOREIGN KEY (room_id)
        REFERENCES chat_rooms (id) ON DELETE CASCADE,
    CONSTRAINT chat_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES user_accounts (id),
    CONSTRAINT chat_messages_content_check CHECK (length(content) BETWEEN 1 AND 1000),
    CONSTRAINT chat_messages_type_check CHECK (
        message_type IN (
            'USER',
            'MEMBER_JOINED',
            'MEMBER_LEFT',
            'MEMBER_INVITED',
            'MEMBER_KICKED',
            'ROOM_RENAMED'
        )
    )
);

-- 최신 메시지부터 커서로 거슬러 올라가는 조회 패턴에 맞춘다.
CREATE INDEX IF NOT EXISTS idx_chat_messages_room ON chat_messages (room_id, id DESC);


-- ============================================================
-- 4. 읽음 위치 (사용자별 방)
-- ============================================================

-- 안 읽은 수 = 그 방에서 last_read_message_id 보다 큰 id 중 내가 보낸 것이 아닌 메시지 수.
CREATE TABLE IF NOT EXISTS chat_reads (
    room_id INT8 NOT NULL,
    user_id INT8 NOT NULL,
    last_read_message_id INT8 NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chat_reads_pkey PRIMARY KEY (room_id, user_id),
    CONSTRAINT chat_reads_room_id_fkey FOREIGN KEY (room_id)
        REFERENCES chat_rooms (id) ON DELETE CASCADE,
    CONSTRAINT chat_reads_user_id_fkey FOREIGN KEY (user_id) REFERENCES user_accounts (id)
);


-- ============================================================
-- 5. 코멘트
-- ============================================================

COMMENT ON TABLE chat_rooms IS '채팅방 (is_public=true 공개 / false 비공개)';
COMMENT ON COLUMN chat_rooms.is_public IS 'true 공개방(전원) / false 비공개방(만든 사람 + 초대된 사람)';
COMMENT ON COLUMN chat_rooms.owner_user_id IS '방을 만든 사용자 (user_accounts.id 참조)';
COMMENT ON COLUMN chat_rooms.name IS '만든 사람이 정한 방 제목';
COMMENT ON COLUMN chat_rooms.active IS 'false 이면 목록에서 감추고 새 메시지를 받지 않는다';

COMMENT ON TABLE chat_participants IS '공개·비공개 채팅방 구성원 이력. 나가면 left_at 을 채우고 행은 보존한다';
COMMENT ON COLUMN chat_participants.joined_at IS '이번 참여의 입장 시각. 조회는 이 시각 이후 메시지만 보여준다';
COMMENT ON COLUMN chat_participants.left_at IS '퇴장 시각. NULL 이면 현재 멤버, 값이 있으면 지난 참여 기록';

COMMENT ON TABLE chat_messages IS '채팅 메시지';
COMMENT ON COLUMN chat_messages.sender_name IS '발신 시점의 표시 이름 스냅샷';
COMMENT ON COLUMN chat_messages.message_type IS 'USER 또는 입장·퇴장·초대·강퇴·방 이름 변경 이벤트 종류';
COMMENT ON COLUMN chat_messages.deleted_at IS '삭제 일시. 값이 있으면 내용을 감춘다(행은 보존)';

COMMENT ON TABLE chat_reads IS '사용자별 방 읽음 위치';


-- ============================================================
-- 6. 권한
-- ============================================================

GRANT SELECT, INSERT, UPDATE, DELETE
    ON TABLE chat_rooms, chat_participants, chat_messages, chat_reads TO lastmission_svc_main;
GRANT USAGE ON SEQUENCE chat_message_id_seq TO lastmission_svc_main;
