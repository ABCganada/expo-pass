package com.coderhan.lastmission.event.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.coderhan.lastmission.event.ReservationQueryPort;
import com.coderhan.lastmission.event.application.command.CreateTicketCommand;
import com.coderhan.lastmission.event.application.command.UpdateTicketCommand;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.event.domain.Ticket;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final ReservationQueryPort reservationQueryPort;
    private final Clock clock;

    /**
     * 티켓 생성 - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional
    public Ticket createTicket(long eventId, long callerUserId, boolean isAdmin, CreateTicketCommand command) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));

        validateEventAccess(event, callerUserId, isAdmin, "티켓을 생성");
        validateTicketCreation(command);

        Ticket ticket = new Ticket(event, command.name(), command.price(), command.quantityTotal(),
                command.maxPurchasePerUser(), command.saleStartAt(), command.saleEndAt());
        return ticketRepository.save(ticket);
    }

    /**
     * 티켓 수정 - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional
    public Ticket updateTicket(long eventId, long ticketId, long callerUserId, boolean isAdmin,
            UpdateTicketCommand command) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        validateEventAccess(event, callerUserId, isAdmin, "티켓을 수정");

        Ticket ticket = ticketRepository.findNotDeletedByIdAndEventId(ticketId, event.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND, "티켓을 찾을 수 없습니다."));
        validateTicketFields(command.name(), command.price(), command.quantityTotal(),
                command.maxPurchasePerUser(), command.saleStartAt(), command.saleEndAt());

        boolean saleAlreadyStarted = !ticket.getSaleStartAt().isAfter(Instant.now(clock));
        if (saleAlreadyStarted && !Objects.equals(command.quantityTotal(), ticket.getQuantityTotal())) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "판매 시작 이후에는 총 수량을 수정할 수 없습니다.");
        }

        ticket.updateDetails(command.name(), command.price(), command.quantityTotal(), command.maxPurchasePerUser(),
                command.saleStartAt(), command.saleEndAt());
        return ticket;
    }

    /**
     * 티켓 삭제 - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional
    public void deleteTicket(long eventId, long ticketId, long callerUserId, boolean isAdmin) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        validateEventAccess(event, callerUserId, isAdmin, "티켓을 삭제");

        Ticket ticket = ticketRepository.findNotDeletedByIdAndEventId(ticketId, event.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND, "티켓을 찾을 수 없습니다."));

        if (reservationQueryPort.hasActiveReservationsForTicket(ticketId)) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "예약 이력이 있는 티켓은 삭제할 수 없습니다.");
        }
        ticket.softDelete(Instant.now(clock));
    }

    /**
     * 행사 티켓 목록 조회 (일반 사용자) - DRAFT 제외, 삭제된 티켓 제외.
     */
    @Transactional(readOnly = true)
    public List<Ticket> getPublicTickets(long eventId) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .filter(candidate -> candidate.getStatus() != EventStatus.DRAFT)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        return ticketRepository.findAllNotDeletedByEventIdOrderByCreatedAtAsc(event.getId());
    }

    /**
     * 행사 티켓 목록 조회 (관리자) - 삭제된 티켓 포함. ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional(readOnly = true)
    public List<Ticket> getAdminTickets(long eventId, long callerUserId, boolean isAdmin) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        validateEventAccess(event, callerUserId, isAdmin, "티켓 목록을 조회");
        return ticketRepository.findAllByEventIdOrderByCreatedAtAsc(event.getId());
    }

    /** ADMIN은 전체 허용, 아니면 본인이 담당(manager_id)하는 행사인지 확인 */
    private void validateEventAccess(Event event, long callerUserId, boolean isAdmin, String action) {
        if (!isAdmin && !Objects.equals(event.getManagerId(), callerUserId)) {
            throw new BusinessException(ErrorCode.EVENT_ACCESS_DENIED, "본인이 담당하는 행사만 " + action + "할 수 있습니다.");
        }
    }

    private void validateTicketCreation(CreateTicketCommand command) {
        validateTicketFields(command.name(), command.price(), command.quantityTotal(),
                command.maxPurchasePerUser(), command.saleStartAt(), command.saleEndAt());
    }

    private void validateTicketFields(String name, Integer price, Integer quantityTotal,
            Integer maxPurchasePerUser, Instant saleStartAt, Instant saleEndAt) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "티켓명은 비어 있을 수 없습니다.");
        }
        if (price == null) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "가격은 필수입니다.");
        }
        if (price < 0) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "가격은 0 이상이어야 합니다.");
        }
        if (quantityTotal == null) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "총 수량은 필수입니다.");
        }
        if (quantityTotal <= 0) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "총 수량은 1 이상이어야 합니다.");
        }
        if (maxPurchasePerUser == null) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "인당 최대 구매 수량은 필수입니다.");
        }
        if (maxPurchasePerUser <= 0 || maxPurchasePerUser > quantityTotal) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "인당 최대 구매 수량이 올바르지 않습니다.");
        }
        if (saleStartAt == null || saleEndAt == null) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "판매 시작/종료 일시는 필수입니다.");
        }
        if (!saleEndAt.isAfter(saleStartAt)) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "판매 종료 일시는 시작 일시보다 늦어야 합니다.");
        }
    }
}