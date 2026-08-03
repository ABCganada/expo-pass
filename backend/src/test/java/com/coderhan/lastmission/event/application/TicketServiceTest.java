package com.coderhan.lastmission.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.coderhan.lastmission.event.ReservationQueryPort;
import com.coderhan.lastmission.event.application.command.CreateTicketCommand;
import com.coderhan.lastmission.event.application.command.UpdateTicketCommand;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.event.domain.Ticket;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    private static final long EVENT_ID = 1L;
    private static final long TICKET_ID = 10L;
    private static final long MANAGER_ID = 100L;
    private static final long OTHER_MANAGER_ID = 200L;
    private static final Instant SALE_START = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant SALE_END = Instant.parse("2026-08-31T00:00:00Z");

    @Mock EventRepository eventRepository;
    @Mock TicketRepository ticketRepository;
    @Mock ReservationQueryPort reservationQueryPort;
    @Spy EventOwnershipValidator ownershipValidator = new EventOwnershipValidator();
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks TicketService service;

    // ── createTicketAsManager / createTicketAsAdmin ────────────────────────────

    @Test
    void createTicketAsManager_담당_매니저는_생성_가능() {
        Event event = event(EventStatus.PUBLISHED);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ticket created = service.createTicketAsManager(EVENT_ID, MANAGER_ID, createCommand());

        assertThat(created.getName()).isEqualTo("일반권");
        assertThat(created.getQuantityRemaining()).isEqualTo(created.getQuantityTotal());
    }

    @Test
    void createTicketAsManager_담당이_아닌_매니저는_거부() {
        Event event = event(EventStatus.PUBLISHED);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.createTicketAsManager(EVENT_ID, OTHER_MANAGER_ID, createCommand()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_ACCESS_DENIED));
    }

    @Test
    void createTicketAsManager_행사가_없으면_예외() {
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTicketAsManager(EVENT_ID, MANAGER_ID, createCommand()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND));
    }

    @Test
    void createTicketAsManager_취소된_행사면_거부() {
        Event event = event(EventStatus.CANCELLED);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.createTicketAsManager(EVENT_ID, MANAGER_ID, createCommand()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.TICKET_NOT_MUTABLE));
    }

    @Test
    void createTicketAsManager_종료된_행사면_거부() {
        Event event = endedPublishedEvent();
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.createTicketAsManager(EVENT_ID, MANAGER_ID, createCommand()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.TICKET_NOT_MUTABLE));
    }

    @Test
    void createTicketAsAdmin_소유권_무관하게_생성_가능() {
        Event event = event(EventStatus.PUBLISHED);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ticket created = service.createTicketAsAdmin(EVENT_ID, createCommand());

        assertThat(created.getName()).isEqualTo("일반권");
    }

    @Test
    void createTicketAsAdmin_취소된_행사면_거부() {
        Event event = event(EventStatus.CANCELLED);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.createTicketAsAdmin(EVENT_ID, createCommand()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.TICKET_NOT_MUTABLE));
    }

    // ── updateTicketAsManager ────────────────────────────────────────────────

    @Test
    void updateTicketAsManager_존재하면_필드_반영() {
        Event event = event(EventStatus.PUBLISHED);
        Ticket ticket = ticket(event, 100, 0);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.findNotDeletedByIdAndEventId(TICKET_ID, EVENT_ID)).thenReturn(Optional.of(ticket));

        Ticket updated = service.updateTicketAsManager(EVENT_ID, TICKET_ID, MANAGER_ID, updateCommand());

        assertThat(updated.getName()).isEqualTo("수정된 티켓");
        assertThat(updated.getPrice()).isEqualTo(20000);
    }

    @Test
    void updateTicketAsManager_삭제된_티켓은_찾지_못함() {
        Event event = event(EventStatus.PUBLISHED);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.findNotDeletedByIdAndEventId(TICKET_ID, EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTicketAsManager(EVENT_ID, TICKET_ID, MANAGER_ID, updateCommand()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    void updateTicketAsManager_판매_시작_전에는_총수량_변경_가능() {
        Event event = event(EventStatus.PUBLISHED);
        Ticket ticket = ticket(event, 100, 0); // SALE_START는 clock 기준 미래
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.findNotDeletedByIdAndEventId(TICKET_ID, EVENT_ID)).thenReturn(Optional.of(ticket));

        Ticket updated = service.updateTicketAsManager(EVENT_ID, TICKET_ID, MANAGER_ID,
                new UpdateTicketCommand("수정된 티켓", 20000, 150, 4, SALE_START, SALE_END));

        assertThat(updated.getQuantityTotal()).isEqualTo(150);
        assertThat(updated.getQuantityRemaining()).isEqualTo(150);
    }

    @Test
    void updateTicketAsManager_판매_시작_후에는_총수량_변경_불가() {
        Event event = event(EventStatus.PUBLISHED);
        Instant pastSaleStart = Instant.parse("2026-07-01T00:00:00Z"); // clock(2026-07-23) 기준 이미 시작됨
        Ticket ticket = ticketWithSaleStart(event, 100, pastSaleStart);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.findNotDeletedByIdAndEventId(TICKET_ID, EVENT_ID)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateTicketAsManager(EVENT_ID, TICKET_ID, MANAGER_ID,
                new UpdateTicketCommand("수정된 티켓", 20000, 150, 4, pastSaleStart, SALE_END)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_INVALID_REQUEST));
        assertThat(ticket.getQuantityTotal()).isEqualTo(100);
    }

    @Test
    void updateTicketAsManager_판매_시작_후에도_총수량이_동일하면_허용() {
        Event event = event(EventStatus.PUBLISHED);
        Instant pastSaleStart = Instant.parse("2026-07-01T00:00:00Z");
        Ticket ticket = ticketWithSaleStart(event, 100, pastSaleStart);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.findNotDeletedByIdAndEventId(TICKET_ID, EVENT_ID)).thenReturn(Optional.of(ticket));

        Ticket updated = service.updateTicketAsManager(EVENT_ID, TICKET_ID, MANAGER_ID,
                new UpdateTicketCommand("수정된 티켓", 20000, 100, 4, pastSaleStart, SALE_END));

        assertThat(updated.getName()).isEqualTo("수정된 티켓");
        assertThat(updated.getQuantityTotal()).isEqualTo(100);
    }

    // ── deleteTicketAsManager ────────────────────────────────────────────────

    @Test
    void deleteTicketAsManager_예약_이력이_없으면_소프트_삭제() {
        Event event = event(EventStatus.PUBLISHED);
        Ticket ticket = ticket(event, 100, 0);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.findNotDeletedByIdAndEventId(TICKET_ID, EVENT_ID)).thenReturn(Optional.of(ticket));

        service.deleteTicketAsManager(EVENT_ID, TICKET_ID, MANAGER_ID);

        assertThat(ticket.isDeleted()).isTrue();
        assertThat(ticket.getDeletedAt()).isEqualTo(Instant.now(clock));
    }

    @Test
    void deleteTicketAsManager_예약_이력이_있으면_거부되고_삭제되지_않음() {
        Event event = event(EventStatus.PUBLISHED);
        Ticket ticket = ticket(event, 100, 0);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.findNotDeletedByIdAndEventId(TICKET_ID, EVENT_ID)).thenReturn(Optional.of(ticket));
        when(reservationQueryPort.hasActiveReservationsForTicket(TICKET_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteTicketAsManager(EVENT_ID, TICKET_ID, MANAGER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_INVALID_REQUEST));
        assertThat(ticket.isDeleted()).isFalse();
    }

    // ── getPublicTickets ─────────────────────────────────────────────────────

    @Test
    void getPublicTickets_게시된_행사는_삭제안된_티켓만_반환() {
        Event event = event(EventStatus.PUBLISHED);
        Ticket ticket = ticket(event, 100, 0);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.findAllNotDeletedByEventIdOrderByCreatedAtAsc(EVENT_ID)).thenReturn(List.of(ticket));

        List<Ticket> tickets = service.getPublicTickets(EVENT_ID);

        assertThat(tickets).containsExactly(ticket);
    }

    @Test
    void getPublicTickets_DRAFT_행사는_조회불가() {
        Event event = event(EventStatus.DRAFT);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.getPublicTickets(EVENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND));
    }

    // ── getManagerTickets / getAdminTickets ─────────────────────────────────

    @Test
    void getAdminTickets_전체_행사_조회_가능() {
        Event event = event(EventStatus.PUBLISHED);
        Ticket ticket = ticket(event, 100, 0);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.findAllByEventIdOrderByCreatedAtAsc(EVENT_ID)).thenReturn(List.of(ticket));

        List<Ticket> tickets = service.getAdminTickets(EVENT_ID);

        assertThat(tickets).containsExactly(ticket);
    }

    @Test
    void getManagerTickets_담당이_아닌_매니저는_거부() {
        Event event = event(EventStatus.PUBLISHED);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.getManagerTickets(EVENT_ID, OTHER_MANAGER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_ACCESS_DENIED));
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private Event event(EventStatus status) {
        Event event = new Event("테스트 행사", category(), MANAGER_ID);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        if (status == EventStatus.PUBLISHED || status == EventStatus.CANCELLED) {
            event.publish();
        }
        if (status == EventStatus.CANCELLED) {
            event.cancel();
        }
        return event;
    }

    /** clock 기준 이미 종료일이 지난 PUBLISHED 행사 */
    private Event endedPublishedEvent() {
        Event event = event(EventStatus.PUBLISHED);
        ReflectionTestUtils.setField(event, "startDate", LocalDate.parse("2026-01-01"));
        ReflectionTestUtils.setField(event, "endDate", LocalDate.parse("2026-01-31"));
        return event;
    }

    private EventCategory category() {
        return new EventCategory("MUSIC", "음악", true);
    }

    private Ticket ticket(Event event, int quantityTotal, int sold) {
        return ticketWithSaleStart(event, quantityTotal, sold, SALE_START);
    }

    private Ticket ticketWithSaleStart(Event event, int quantityTotal, Instant saleStartAt) {
        return ticketWithSaleStart(event, quantityTotal, 0, saleStartAt);
    }

    private Ticket ticketWithSaleStart(Event event, int quantityTotal, int sold, Instant saleStartAt) {
        Ticket ticket = new Ticket(event, "일반권", 10000, quantityTotal, 4, saleStartAt, SALE_END);
        ReflectionTestUtils.setField(ticket, "id", TICKET_ID);
        if (sold > 0) {
            ReflectionTestUtils.setField(ticket, "quantityRemaining", quantityTotal - sold);
        }
        return ticket;
    }

    private CreateTicketCommand createCommand() {
        return new CreateTicketCommand("일반권", 10000, 100, 4, SALE_START, SALE_END);
    }

    private UpdateTicketCommand updateCommand() {
        return new UpdateTicketCommand("수정된 티켓", 20000, 100, 4, SALE_START, SALE_END);
    }
}