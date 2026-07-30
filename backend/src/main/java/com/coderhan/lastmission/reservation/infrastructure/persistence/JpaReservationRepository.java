package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.coderhan.lastmission.reservation.application.ReservationRepository;
import com.coderhan.lastmission.reservation.domain.OrderStatus;
import com.coderhan.lastmission.reservation.domain.QrTicketView;
import com.coderhan.lastmission.reservation.domain.ReservationOrder;
import com.coderhan.lastmission.reservation.domain.ReservationOrderItem;
import com.coderhan.lastmission.reservation.domain.TicketQuantity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaReservationRepository implements ReservationRepository {
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ReservationOrderJpaRepository orderJpaRepository;
    private final ReservationOrderItemJpaRepository itemJpaRepository;
    private final EntityManager entityManager;

    @Override
    public String nextOrderId(LocalDate today) {
        Number sequenceValue = (Number) entityManager
                .createNativeQuery("select nextval('reservation_order_seq')")
                .getSingleResult();
        return "ORD-" + today.format(ORDER_DATE_FORMAT) + "-" + String.format("%06d", sequenceValue.longValue());
    }

    @Override
    public ReservationOrder createOrder(String orderId, long userId, long eventId, BigDecimal totalAmount,
                                        OffsetDateTime now) {
        ReservationOrderEntity saved = orderJpaRepository.save(
                new ReservationOrderEntity(orderId, userId, eventId, OrderStatus.PENDING, totalAmount, now, now));
        return toDomain(saved);
    }

    @Override
    public ReservationOrderItem addItem(String orderId, long ticketId, BigDecimal unitPrice, String qrCodeHash) {
        ReservationOrderItemEntity saved = itemJpaRepository.save(
                new ReservationOrderItemEntity(orderId, ticketId, unitPrice, qrCodeHash));
        return toDomain(saved);
    }

    @Override
    public Optional<ReservationOrder> findOrder(String orderId) {
        return orderJpaRepository.findById(orderId).map(JpaReservationRepository::toDomain);
    }

    @Override
    public List<ReservationOrderItem> findItems(String orderId) {
        return itemJpaRepository.findByOrderId(orderId).stream()
                .map(JpaReservationRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<ReservationOrderItem> findItemByQrCodeHash(String qrCodeHash) {
        return itemJpaRepository.findByQrCodeHash(qrCodeHash).map(JpaReservationRepository::toDomain);
    }

    @Override
    public boolean checkin(String qrCodeHash, long adminUserId, OffsetDateTime now) {
        return itemJpaRepository.checkin(qrCodeHash, adminUserId, now) > 0;
    }

    @Override
    public List<ReservationOrder> findOrdersByUserId(long userId) {
        return orderJpaRepository.findByUserIdOrderByReservedAtDesc(userId).stream()
                .map(JpaReservationRepository::toDomain)
                .toList();
    }

    @Override
    public List<ReservationOrder> findOrdersByEventId(long eventId) {
        return orderJpaRepository.findByEventIdOrderByReservedAtDesc(eventId).stream()
                .map(JpaReservationRepository::toDomain)
                .toList();
    }

    @Override
    public Map<OrderStatus, Long> countOrdersByEventIdGroupedByStatus(long eventId) {
        return orderJpaRepository.countByEventIdGroupByStatus(eventId).stream()
                .collect(Collectors.toMap(ReservationOrderJpaRepository.StatusCount::getStatus,
                        ReservationOrderJpaRepository.StatusCount::getCount));
    }

    @Override
    public long countPurchasedQuantity(long userId, long ticketId) {
        return itemJpaRepository.countByUserIdAndTicketId(userId, ticketId);
    }

    @Override
    public Map<String, List<TicketQuantity>> findTicketQuantitiesByUserId(long userId) {
        return itemJpaRepository.findTicketQuantitiesByUserId(userId).stream()
                .collect(Collectors.groupingBy(
                        ReservationOrderItemJpaRepository.OrderTicketQuantityRow::getOrderId,
                        Collectors.mapping(
                                row -> new TicketQuantity(row.getTicketId(), (int) row.getQuantity()),
                                Collectors.toList())));
    }

    @Override
    public List<QrTicketView> findQrTicketsByUserId(long userId) {
        return itemJpaRepository.findQrTicketsByUserId(userId).stream()
                .map(row -> new QrTicketView(row.getOrderId(), row.getEventId(), row.getOrderItemId(),
                        row.getTicketId(), row.getQrCodeHash(), row.getCheckedInAt()))
                .toList();
    }

    private static ReservationOrder toDomain(ReservationOrderEntity entity) {
        return new ReservationOrder(entity.getOrderId(), entity.getUserId(), entity.getEventId(),
                entity.getStatus(), entity.getTotalAmount(), entity.getReservedAt(), entity.getUpdatedAt());
    }

    private static ReservationOrderItem toDomain(ReservationOrderItemEntity entity) {
        return new ReservationOrderItem(entity.getOrderItemId(), entity.getOrderId(), entity.getTicketId(),
                entity.getUnitPrice(), entity.getQrCodeHash(), entity.getCheckedInAt());
    }
}