package com.coderhan.lastmission.event.application;

import java.util.Objects;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
class EventOwnershipValidator {

    void requireOwner(Event event, long callerUserId, String action) {
        if (!Objects.equals(event.getManagerId(), callerUserId)) {
            throw new BusinessException(ErrorCode.EVENT_ACCESS_DENIED, "본인이 담당하는 행사만 " + action + "할 수 있습니다.");
        }
    }
}