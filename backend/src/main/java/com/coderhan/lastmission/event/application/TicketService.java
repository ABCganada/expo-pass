package com.coderhan.lastmission.event.application;

import java.util.Objects;

import com.coderhan.lastmission.event.application.command.CreateTicketCommand;
import com.coderhan.lastmission.event.domain.Event;
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

    /** ADMIN은 전체 허용, 아니면 본인이 담당(manager_id)하는 행사인지 확인 */
    private void validateEventAccess(Event event, long callerUserId, boolean isAdmin, String action) {
        if (!isAdmin && !Objects.equals(event.getManagerId(), callerUserId)) {
            throw new BusinessException(ErrorCode.EVENT_ACCESS_DENIED, "본인이 담당하는 행사만 " + action + "할 수 있습니다.");
        }
    }

    private void validateTicketCreation(CreateTicketCommand command) {
        if (command.name() == null || command.name().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "티켓명은 비어 있을 수 없습니다.");
        }
        if (command.price() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "가격은 필수입니다.");
        }
        if (command.price() < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "가격은 0 이상이어야 합니다.");
        }
        if (command.quantityTotal() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "총 수량은 필수입니다.");
        }
        if (command.quantityTotal() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "총 수량은 1 이상이어야 합니다.");
        }
        if (command.maxPurchasePerUser() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "인당 최대 구매 수량은 필수입니다.");
        }
        if (command.maxPurchasePerUser() <= 0 || command.maxPurchasePerUser() > command.quantityTotal()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "인당 최대 구매 수량이 올바르지 않습니다.");
        }
        if (command.saleStartAt() == null || command.saleEndAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "판매 시작/종료 일시는 필수입니다.");
        }
        if (!command.saleEndAt().isAfter(command.saleStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "판매 종료 일시는 시작 일시보다 늦어야 합니다.");
        }
    }
}