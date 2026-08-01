package com.coderhan.lastmission.event.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.event.application.EventRepository;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EventJpaRepository extends JpaRepository<Event, Long>, EventRepository {

    @Override
    @Query("""
        SELECT e FROM Event e
        JOIN FETCH e.category
        WHERE e.id = :id AND e.deletedAt IS NULL
    """)
    Optional<Event> findNotDeletedById(@Param("id") long id);

    @Override
    @Query("""
        SELECT e FROM Event e
        JOIN FETCH e.category
        WHERE e.status = :status AND e.deletedAt IS NULL
        ORDER BY e.startDate ASC
    """)
    List<Event> findByStatusOrderByStartDateAsc(@Param("status") EventStatus status);

    @Override
    @Query("""
        SELECT e FROM Event e
        JOIN FETCH e.category
        WHERE e.status = :status AND e.category.id = :categoryId AND e.deletedAt IS NULL
        ORDER BY e.startDate ASC
    """)
    List<Event> findByStatusAndCategoryIdOrderByStartDateAsc(
            @Param("status") EventStatus status, @Param("categoryId") long categoryId);

    @Override
    @Query("""
        SELECT e FROM Event e
        JOIN FETCH e.category
        WHERE e.deletedAt IS NULL
        ORDER BY e.startDate ASC
    """)
    List<Event> findAllOrderByStartDateAsc();

    @Override
    @Query("""
        SELECT e FROM Event e
        WHERE e.status = com.coderhan.lastmission.event.domain.EventStatus.PUBLISHED
        AND e.deletedAt IS NULL
        AND e.endDate < :date
        AND e.endedNotifiedAt IS NULL
    """)
    List<Event> findEndedEventsNotNotified(@Param("date") LocalDate date);

    @Override
    @Query("""
        SELECT e.id FROM Event e
        WHERE e.managerId = :managerId 
        AND e.deletedAt IS NULL
    """)
    List<Long> findIdsByManagerId(@Param("managerId") long managerId);

    @Override
    @Query("""
        SELECT e.startDate FROM Event e
        WHERE e.id = :eventId
        AND e.deletedAt IS NULL
    """)
    Optional<LocalDate> findStartDateByEventId(@Param("eventId") long eventId);

    @Override
    @Modifying
    @Query("""
        UPDATE Event e
        SET e.viewCount = e.viewCount + 1
        WHERE e.id = :eventId
        AND e.deletedAt IS NULL
        AND e.status <> com.coderhan.lastmission.event.domain.EventStatus.DRAFT
    """)
    int increaseViewCount(@Param("eventId") long eventId);
}