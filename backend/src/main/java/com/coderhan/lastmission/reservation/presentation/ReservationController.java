package com.coderhan.lastmission.reservation.presentation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import com.coderhan.lastmission.reservation.application.ReservationService;
import com.coderhan.lastmission.reservation.domain.OrderStatus;
import com.coderhan.lastmission.reservation.domain.ReservationOrder;
import com.coderhan.lastmission.reservation.domain.ReservationOrderItem;
import com.coderhan.lastmission.shared.ApiResponse;
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
