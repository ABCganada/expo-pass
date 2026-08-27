package com.coderhan.lastmission.chat.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.chat.application.ChatRepository;
import com.coderhan.lastmission.chat.domain.ChatMessage;
import com.coderhan.lastmission.chat.domain.ChatMessageType;
import com.coderhan.lastmission.chat.domain.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 채팅 저장소. chat_* 테이블만 읽고 쓴다.
 *
 * <p>사용자 이메일 조회는 user 모듈의 공개 API가 담당한다. 이 저장소는 참가자 ID와
 * 발신 시점 이름 스냅샷만 다룬다.</p>
 */
@Repository
@RequiredArgsConstructor
class JdbcChatRepository implements ChatRepository {
    private static final RowMapper<ChatRoom> ROOM_MAPPER = JdbcChatRepository::mapRoom;
    private static final RowMapper<RoomSummary> SUMMARY_MAPPER = JdbcChatRepository::mapSummary;
    private static final RowMapper<ChatMessage> MESSAGE_MAPPER = JdbcChatRepository::mapMessage;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<ChatRoom> findRoom(long roomId) {
        return jdbcTemplate.query("""
                SELECT id, is_public, owner_user_id, name, active, created_at
                FROM chat_rooms WHERE id = ?
                """, ROOM_MAPPER, roomId).stream().findFirst();
    }

    @Override
    public List<RoomSummary> findPublicRooms(long userId) {
        return jdbcTemplate.query("""
                SELECT * FROM (
                    SELECT r.id, r.is_public, r.owner_user_id, r.name, r.active, r.created_at,
                       CASE WHEN p.room_id IS NULL OR rd.room_id IS NULL THEN 0 ELSE (
                           SELECT count(*) FROM chat_messages um
                           WHERE um.room_id = r.id
                             AND um.sender_id <> ?
                             AND um.created_at >= p.joined_at
                             AND um.id > rd.last_read_message_id
                       ) END AS unread_count,
                       (SELECT CASE WHEN lm.deleted_at IS NULL THEN lm.content ELSE '삭제된 메시지입니다.' END
                        FROM chat_messages lm
                        WHERE lm.room_id = r.id
                          AND p.room_id IS NOT NULL
                          AND lm.created_at >= p.joined_at
                        ORDER BY lm.id DESC LIMIT 1) AS last_message,
                       (SELECT lm.created_at FROM chat_messages lm
                        WHERE lm.room_id = r.id
                          AND p.room_id IS NOT NULL
                          AND lm.created_at >= p.joined_at
                        ORDER BY lm.id DESC LIMIT 1) AS last_message_at
                FROM chat_rooms r
                LEFT JOIN chat_participants p
                  ON p.room_id = r.id AND p.user_id = ? AND p.left_at IS NULL
                LEFT JOIN chat_reads rd ON rd.room_id = r.id AND rd.user_id = ?
                WHERE r.is_public = true AND r.active = true
                ) room_summary
                ORDER BY coalesce(room_summary.last_message_at, room_summary.created_at) DESC,
                         room_summary.id DESC
                """, SUMMARY_MAPPER, userId, userId, userId);
    }

    @Override
    public List<RoomSummary> findPrivateRooms(long userId) {
        return jdbcTemplate.query("""
                SELECT * FROM (
                    SELECT r.id, r.is_public, r.owner_user_id, r.name, r.active, r.created_at,
                       (SELECT count(*) FROM chat_messages um
                        WHERE um.room_id = r.id
                          AND um.sender_id <> ?
                          AND um.created_at >= p.joined_at
                          AND um.id > coalesce(rd.last_read_message_id, 0)) AS unread_count,
                       (SELECT CASE WHEN lm.deleted_at IS NULL THEN lm.content ELSE '삭제된 메시지입니다.' END
                        FROM chat_messages lm
                        WHERE lm.room_id = r.id AND lm.created_at >= p.joined_at
                        ORDER BY lm.id DESC LIMIT 1) AS last_message,
                       (SELECT lm.created_at FROM chat_messages lm
                        WHERE lm.room_id = r.id AND lm.created_at >= p.joined_at
                        ORDER BY lm.id DESC LIMIT 1) AS last_message_at
                FROM chat_rooms r
                JOIN chat_participants p
                  ON p.room_id = r.id AND p.user_id = ? AND p.left_at IS NULL
                LEFT JOIN chat_reads rd ON rd.room_id = r.id AND rd.user_id = p.user_id
                WHERE r.is_public = false AND r.active = true
                ) room_summary
                ORDER BY coalesce(room_summary.last_message_at, room_summary.created_at) DESC,
                         room_summary.id DESC
                """, SUMMARY_MAPPER, userId, userId);
    }

    @Override
    public ChatRoom createRoom(long ownerUserId, String name, boolean isPublic, OffsetDateTime now) {
        return jdbcTemplate.query("""
                INSERT INTO chat_rooms (is_public, owner_user_id, name, created_at)
                VALUES (?, ?, ?, ?)
                RETURNING id, is_public, owner_user_id, name, active, created_at
                """, ROOM_MAPPER, isPublic, ownerUserId, name, now)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("채팅방을 만들지 못했습니다."));
    }

    @Override
    public boolean isCurrentParticipant(long roomId, long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM chat_participants
                WHERE room_id = ? AND user_id = ? AND left_at IS NULL
                """, Integer.class, roomId, userId);
        return count != null && count > 0;
    }

    @Override
    public List<Long> findCurrentParticipantIds(long roomId) {
        return jdbcTemplate.query("""
                SELECT user_id FROM chat_participants
                WHERE room_id = ? AND left_at IS NULL
                ORDER BY joined_at, id
                """, (rs, rowNum) -> rs.getLong("user_id"), roomId);
    }

    @Override
    public Optional<OffsetDateTime> findCurrentJoinedAt(long roomId, long userId) {
        return jdbcTemplate.query("""
                SELECT joined_at FROM chat_participants
                WHERE room_id = ? AND user_id = ? AND left_at IS NULL
                """, (rs, rowNum) -> rs.getObject("joined_at", OffsetDateTime.class), roomId, userId)
                .stream().findFirst();
    }

    @Override
    public void addParticipant(long roomId, long userId, OffsetDateTime joinedAt) {
        jdbcTemplate.update("""
                INSERT INTO chat_participants (room_id, user_id, joined_at)
                VALUES (?, ?, ?)
                """, roomId, userId, joinedAt);
    }

    @Override
    public boolean leaveParticipant(long roomId, long userId, OffsetDateTime leftAt) {
        return jdbcTemplate.update("""
                UPDATE chat_participants SET left_at = ?
                WHERE room_id = ? AND user_id = ? AND left_at IS NULL
                """, leftAt, roomId, userId) > 0;
    }

    @Override
    public List<ChatMessage> findMessages(long roomId, OffsetDateTime visibleAfter, Long beforeId, int limit) {
        List<Object> args = new ArrayList<>();
        args.add(roomId);
        StringBuilder sql = new StringBuilder("""
                SELECT id, room_id, sender_id, sender_name, content,
                       message_type, created_at, deleted_at
                FROM chat_messages
                WHERE room_id = ?
                """);
        if (visibleAfter != null) {
            sql.append(" AND created_at >= ? ");
            args.add(visibleAfter);
        }
        if (beforeId != null) {
            sql.append(" AND id < ? ");
            args.add(beforeId);
        }
        sql.append(" ORDER BY id DESC LIMIT ? ");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), MESSAGE_MAPPER, args.toArray());
    }

    @Override
    public ChatMessage save(long roomId, long senderId, String senderName, String content,
            OffsetDateTime sentAt) {
        return jdbcTemplate.query("""
                INSERT INTO chat_messages (
                    room_id, sender_id, sender_name, content, message_type, created_at
                ) VALUES (?, ?, ?, ?, 'USER', ?)
                RETURNING id, room_id, sender_id, sender_name, content,
                          message_type, created_at, deleted_at
                """, MESSAGE_MAPPER, roomId, senderId, senderName, content, sentAt)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("메시지를 저장하지 못했습니다."));
    }

    @Override
    public ChatMessage saveSystemEvent(
            long roomId, long actorUserId, ChatMessageType messageType,
            String content, OffsetDateTime occurredAt) {
        return jdbcTemplate.query("""
                INSERT INTO chat_messages (
                    room_id, sender_id, sender_name, content, message_type, created_at
                ) VALUES (?, ?, '시스템', ?, ?, ?)
                RETURNING id, room_id, sender_id, sender_name, content,
                          message_type, created_at, deleted_at
                """, MESSAGE_MAPPER, roomId, actorUserId, content, messageType.name(), occurredAt)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("시스템 메시지를 저장하지 못했습니다."));
    }

    @Override
    public long findLatestMessageId(long roomId) {
        Long latest = jdbcTemplate.queryForObject(
                "SELECT coalesce(max(id), 0) FROM chat_messages WHERE room_id = ?", Long.class, roomId);
        return latest == null ? 0L : latest;
    }

    @Override
    public int countUnread(long roomId, long userId, OffsetDateTime visibleAfter,
            boolean requireReadMarker) {
        if (requireReadMarker) {
            Integer unread = jdbcTemplate.queryForObject("""
                    SELECT CASE WHEN EXISTS (
                        SELECT 1 FROM chat_reads WHERE room_id = ? AND user_id = ?
                    ) THEN (
                        SELECT count(*) FROM chat_messages m
                        WHERE m.room_id = ? AND m.sender_id <> ?
                          AND m.created_at >= ?
                          AND m.id > coalesce((
                              SELECT last_read_message_id FROM chat_reads
                              WHERE room_id = ? AND user_id = ?
                          ), 0)
                    ) ELSE 0 END
                    """, Integer.class, roomId, userId, roomId, userId, visibleAfter,
                    roomId, userId);
            return unread == null ? 0 : unread;
        }

        Integer unread = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM chat_messages m
                WHERE m.room_id = ? AND m.sender_id <> ?
                  AND m.created_at >= ?
                  AND m.id > coalesce((
                      SELECT last_read_message_id FROM chat_reads
                      WHERE room_id = ? AND user_id = ?
                  ), 0)
                """, Integer.class, roomId, userId, visibleAfter, roomId, userId);
        return unread == null ? 0 : unread;
    }

    @Override
    public void markRead(long roomId, long userId, long lastReadMessageId, OffsetDateTime readAt) {
        // 읽음 위치는 뒤로 가지 않는다.
        jdbcTemplate.update("""
                UPSERT INTO chat_reads (room_id, user_id, last_read_message_id, updated_at)
                SELECT ?, ?, greatest(?, coalesce(
                    (SELECT last_read_message_id FROM chat_reads WHERE room_id = ? AND user_id = ?), 0)), ?
                """, roomId, userId, lastReadMessageId, roomId, userId, readAt);
    }

    @Override
    public void clearReadPosition(long roomId, long userId) {
        jdbcTemplate.update("""
                DELETE FROM chat_reads
                WHERE room_id = ? AND user_id = ?
                """, roomId, userId);
    }

    private static ChatRoom mapRoom(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ChatRoom(
                resultSet.getLong("id"),
                resultSet.getBoolean("is_public"),
                resultSet.getLong("owner_user_id"),
                resultSet.getString("name"),
                resultSet.getBoolean("active"),
                resultSet.getObject("created_at", OffsetDateTime.class));
    }

    private static RoomSummary mapSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RoomSummary(
                mapRoom(resultSet, rowNumber),
                resultSet.getInt("unread_count"),
                resultSet.getString("last_message"),
                resultSet.getObject("last_message_at", OffsetDateTime.class));
    }

    private static ChatMessage mapMessage(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ChatMessage(
                resultSet.getLong("id"),
                resultSet.getLong("room_id"),
                resultSet.getLong("sender_id"),
                resultSet.getString("sender_name"),
                resultSet.getString("content"),
                ChatMessageType.valueOf(resultSet.getString("message_type")),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("deleted_at", OffsetDateTime.class));
    }
}
