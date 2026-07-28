package com.coderhan.lastmission.event.application;

import java.util.Optional;
import com.coderhan.lastmission.event.EventDirectory;
import com.coderhan.lastmission.event.TicketInfo;
import com.coderhan.lastmission.event.domain.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class TicketStockService implements EventDirectory {
    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketInfo> getTicketInfo(long ticketId) {
        return ticketRepository.findAvailableById(ticketId).map(TicketStockService::toTicketInfo);
    }

    @Override
    @Transactional
    public boolean decreaseTicketStock(long ticketId, int quantity) {
        return ticketRepository.decreaseTicketStock(ticketId, quantity) > 0;
    }

    private static TicketInfo toTicketInfo(Ticket ticket) {
        return new TicketInfo(ticket.getId(), ticket.getPrice(), ticket.getMaxPurchasePerUser(),
                ticket.getSaleStartAt(), ticket.getSaleEndAt());
    }
}