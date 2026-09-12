package com.coderhan.lastmission.payment.infrastructure.schedule;

import java.time.LocalDate;
import com.coderhan.lastmission.payment.application.EventScheduleReader;
import org.springframework.stereotype.Component;

/**
 * {@link EventScheduleReader}의 임시 구현.
 *
 * TODO reservation/event 도메인과 실제 조회 연동으로 교체할 것. 그 전까지는 행사 시작일을
 * 알 수 없으므로, 자동승인 분기를 타지 않도록(=항상 수동승인 대상) 어제 날짜를 반환한다 —
 * 실제 행사 날짜 없이 환불을 자동 완료 처리하는 쪽보다 관리자 검토로 넘기는 쪽이 안전하다.
 */
@Component
class TodoEventScheduleReader implements EventScheduleReader {

    @Override
    public LocalDate findEventStartDate(String orderId) {
        return LocalDate.now().minusDays(1);
    }
}
