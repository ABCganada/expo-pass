package com.coderhan.lastmission.event.infrastructure.persistence;

import java.util.Optional;
import com.coderhan.lastmission.event.application.TicketRepository;
import com.coderhan.lastmission.event.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TicketJpaRepository extends JpaRepository<Ticket, Long>, TicketRepository {

    @Override
    @Query("""
        SELECT t FROM Ticket t JOIN t.event e
        WHERE t.id = :id AND t.deletedAt IS NULL AND e.deletedAt IS NULL
              AND e.status = com.coderhan.lastmission.event.domain.EventStatus.PUBLISHED
    """)
    Optional<Ticket> findAvailableById(@Param("id") long id);

    @Override
    @Query("""
        SELECT t FROM Ticket t 
        JOIN t.event e
        WHERE t.id = :id AND e.id = :eventId AND t.deletedAt IS NULL
    """)
    Optional<Ticket> findNotDeletedByIdAndEventId(@Param("id") Long id, @Param("eventId") Long eventId);

    @Override
    @Modifying
    @Query("""
        UPDATE Ticket t SET t.quantityRemaining = t.quantityRemaining - :quantity
        WHERE t.id = :id
            AND t.deletedAt IS NULL
            AND t.quantityRemaining >= :quantity
            AND t.saleStartAt <= CURRENT_TIMESTAMP
            AND t.saleEndAt >= CURRENT_TIMESTAMP
            AND EXISTS (
                SELECT 1
                FROM Event e
                WHERE e = t.event
                AND e.deletedAt IS NULL
                AND e.status = com.coderhan.lastmission.event.domain.EventStatus.PUBLISHED
            )
    """)
    int decreaseTicketStock(@Param("id") long ticketId, @Param("quantity") int quantity);

    @Override
    @Modifying
    @Query("""
        UPDATE Ticket t SET t.quantityRemaining = t.quantityRemaining + :quantity
        WHERE t.id = :id
            AND t.deletedAt IS NULL
            AND t.quantityRemaining + :quantity <= t.quantityTotal
    """)
    int increaseTicketStock(@Param("id") long ticketId, @Param("quantity") int quantity);
}