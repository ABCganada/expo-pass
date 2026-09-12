package com.coderhan.lastmission.event.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.event.application.EventRepository;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
        WHERE e.deletedAt IS NULL
        ORDER BY e.startDate ASC
    """)
    List<Event> findAllOrderByStartDateAsc();
}