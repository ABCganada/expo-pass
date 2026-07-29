package com.coderhan.lastmission.event.application;

import java.util.Optional;
import com.coderhan.lastmission.event.domain.Ticket;

public interface TicketRepository {
    Ticket save(Ticket ticket);

    Optional<Ticket> findByIdAndEventId(Long id, Long eventId);

    Optional<Ticket> findAvailableById(long id);
    
    int decreaseTicketStock(long ticketId, int quantity);
}