package com.coderhan.lastmission.shared.web;

import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ApiExceptionHandler {
    record ErrorResponse(boolean success, String errorCode, String message) {}

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        HttpStatus status = switch (exception.errorCode()) {
            case CHAT_ROOM_NOT_FOUND, CHAT_MESSAGE_NOT_FOUND, CHAT_USER_NOT_FOUND,
                    RESERVATION_NOT_FOUND, BANNER_SLOT_NOT_FOUND, BANNER_AD_NOT_FOUND,
                    ADMIN_MEMBER_NOT_FOUND, EVENT_NOT_FOUND, PAYMENT_NOT_FOUND,
                    EVENT_CATEGORY_NOT_FOUND, EVENT_MANAGER_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;

            case CHAT_ACCESS_DENIED, RESERVATION_ACCESS_DENIED,
                    RESERVATION_WAITING_ROOM_REQUIRED, BANNER_AD_ACCESS_DENIED,
                    EVENT_ACCESS_DENIED, PAYMENT_ACCESS_DENIED ->
                    HttpStatus.FORBIDDEN;
            case INVALID_REQUEST, CHAT_MESSAGE_INVALID, CHAT_ROOM_INVALID,
                    RESERVATION_INVALID_REQUEST, BANNER_AD_INVALID_REQUEST,
                    BANNER_AD_ALREADY_REVIEWED, ADMIN_ROLE_INVALID,
                    PAYMENT_INVALID_REQUEST, PAYMENT_CONFIRM_FAILED,
                    BANNER_STAT_INVALID_DATE_RANGE ->
                    HttpStatus.BAD_REQUEST;

            case CHAT_PARTICIPANT_EXISTS, ADMIN_SELF_DEMOTION ->
                    HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(
                new ErrorResponse(false, exception.errorCode().name(), exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleInvalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(false, ErrorCode.INVALID_REQUEST.name(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(false, ErrorCode.INVALID_REQUEST.name(),
                exception.getName() + " 값이 올바르지 않습니다."));
    }
}
