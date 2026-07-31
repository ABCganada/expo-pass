package com.coderhan.lastmission.payment.application;

import java.util.List;

/**
 * 정산 조회 시 "본인이 담당하는 행사인지" 검사하기 위한 포트.
 *
 * TODO 실제로는 event 도메인에서 events.manager_id로 조회하면 된다(Event 자체 테이블에
 * 있는 컬럼). Event는 이 도메인 소유가 아니므로 직접 구현을 추가할 수 없다 — 담당자와
 * 인터페이스 협의가 끝나기 전까지는 임시 구현({@code infrastructure.schedule} 패키지)을
 * 사용한다.
 */
public interface EventManagerLookup {
    /** 상세 조회용(정방향): eventId 하나의 담당 관리자. 알 수 없으면 null. */
    Long findEventManagerId(long eventId);

    /** 목록 조회용(역방향): managerId가 담당하는 행사 id 목록. */
    List<Long> findEventIdsManagedBy(long managerId);
}
