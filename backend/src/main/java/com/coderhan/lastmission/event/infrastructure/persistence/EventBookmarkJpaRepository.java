package com.coderhan.lastmission.event.infrastructure.persistence;

import java.util.List;
import com.coderhan.lastmission.event.application.EventBookmarkRepository;
import com.coderhan.lastmission.event.domain.EventBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EventBookmarkJpaRepository extends JpaRepository<EventBookmark, Long>, EventBookmarkRepository {

    @Override
    void deleteAllByEventId(long eventId);

    @Override
    @Query("""
        SELECT b FROM EventBookmark b
        JOIN FETCH b.event e
        JOIN FETCH e.category
        WHERE b.userId = :userId
        ORDER BY b.createdAt DESC
    """)
    List<EventBookmark> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") long userId);
}