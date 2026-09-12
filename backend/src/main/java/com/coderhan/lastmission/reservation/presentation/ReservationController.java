package com.coderhan.lastmission.reservation.presentation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import com.coderhan.lastmission.reservation.application.ReservationService;
import com.coderhan.lastmission.reservation.domain.OrderStatus;
import com.coderhan.lastmission.reservation.domain.QrTicketView;
import com.coderhan.lastmission.reservation.domain.ReservationOrder;
import com.coderhan.lastmission.reservation.domain.ReservationOrderItem;
import com.coderhan.lastmission.reservation.domain.TicketQuantity;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예약 주문 API.
 *
 * <p>ID 는 문자열로 내보낸다(프로젝트 공통 규약 — CockroachDB int8 이 2^53 을 넘어 JS Number
 * 로는 정확히 표현되지 않는다). {@code orderId} 는 원래부터 문자열(예: ORD-20260727-000001)이다.</p>
 */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        List<ReservationService.OrderItemRequest> items = request == null || request.items() == null
                ? List.of()
                : request.items().stream().map(OrderItemRequest::toServiceRequest).toList();
        ReservationService.OrderDetail detail = reservationService.createOrder(
                principal.userId(), request == null ? 0 : request.eventId(), items);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(OrderResponse.from(detail)));
    }

    @GetMapping("/me")
    ApiResponse<OrdersPageResponse> getMyOrders(@AuthenticationPrincipal LastMissionPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        ReservationService.MyOrdersPage result = reservationService.getMyOrders(
                principal.userId(), parseStatus(status), page, size);
        return ApiResponse.success(OrdersPageResponse.from(result));
    }

    private OrderStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "status 값이 올바르지 않습니다.");
        }
    }

    @GetMapping("/me/qr-tickets")
    ApiResponse<List<QrTicketResponse>> getMyQrTickets(@AuthenticationPrincipal LastMissionPrincipal principal) {
        List<QrTicketView> tickets = reservationService.getMyQrTickets(principal.userId());
        return ApiResponse.success(tickets.stream().map(QrTicketResponse::from).toList());
    }

    @GetMapping("/{orderId}")
    ApiResponse<OrderResponse> getOrder(@PathVariable String orderId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        ReservationService.OrderDetail detail = reservationService.getOrder(principal.userId(), orderId);
        return ApiResponse.success(OrderResponse.from(detail));
    }

    record OrderItemRequest(long ticketId, BigDecimal unitPrice, int quantity) {
        ReservationService.OrderItemRequest toServiceRequest() {
            return new ReservationService.OrderItemRequest(ticketId, unitPrice, quantity);
        }
    }

    record CreateOrderRequest(long eventId, List<OrderItemRequest> items) {}

    record TicketQuantityResponse(String ticketId, int quantity) {
        static TicketQuantityResponse from(TicketQuantity ticketQuantity) {
            return new TicketQuantityResponse(Long.toString(ticketQuantity.ticketId()), ticketQuantity.quantity());
        }
    }

    record OrderSummary(
            String orderId, String eventId, OrderStatus status, BigDecimal totalAmount, OffsetDateTime reservedAt,
            List<TicketQuantityResponse> ticketQuantities
    ) {
        static OrderSummary from(ReservationService.OrderWithTickets orderWithTickets) {
            ReservationOrder order = orderWithTickets.order();
            return new OrderSummary(order.orderId(), Long.toString(order.eventId()), order.status(),
                    order.totalAmount(), order.reservedAt(),
                    orderWithTickets.ticketQuantities().stream().map(TicketQuantityResponse::from).toList());
        }
    }

    record OrdersPageResponse(
            List<OrderSummary> orders, int page, int size, long totalElements, int totalPages
    ) {
        static OrdersPageResponse from(ReservationService.MyOrdersPage pageResult) {
            List<OrderSummary> orders = pageResult.orders().stream().map(OrderSummary::from).toList();
            int totalPages = pageResult.size() == 0
                    ? 0
                    : (int) Math.ceil((double) pageResult.totalElements() / pageResult.size());
            return new OrdersPageResponse(orders, pageResult.page(), pageResult.size(),
                    pageResult.totalElements(), totalPages);
        }
    }

    record QrTicketResponse(
            String orderId, String eventId, String orderItemId, String ticketId, String qrCodeHash,
            OffsetDateTime checkedInAt
    ) {
        static QrTicketResponse from(QrTicketView view) {
            return new QrTicketResponse(view.orderId(), Long.toString(view.eventId()),
                    Long.toString(view.orderItemId()), Long.toString(view.ticketId()), view.qrCodeHash(),
                    view.checkedInAt());
        }
    }

    record OrderItemResponse(String orderItemId, String ticketId, BigDecimal unitPrice, String qrCodeHash,
            OffsetDateTime checkedInAt) {
        static OrderItemResponse from(ReservationOrderItem item) {
            return new OrderItemResponse(Long.toString(item.orderItemId()), Long.toString(item.ticketId()),
                    item.unitPrice(), item.qrCodeHash(), item.checkedInAt());
        }
    }

    record OrderResponse(
            String orderId, String userId, String eventId, OrderStatus status, BigDecimal totalAmount,
            OffsetDateTime reservedAt, List<OrderItemResponse> items
    ) {
        static OrderResponse from(ReservationService.OrderDetail detail) {
            ReservationOrder order = detail.order();
            return new OrderResponse(order.orderId(), Long.toString(order.userId()),
                    Long.toString(order.eventId()), order.status(), order.totalAmount(), order.reservedAt(),
                    detail.items().stream().map(OrderItemResponse::from).toList());
        }
    }
}
