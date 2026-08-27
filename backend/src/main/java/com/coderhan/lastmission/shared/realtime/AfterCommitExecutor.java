package com.coderhan.lastmission.shared.realtime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * DB 변경과 함께 발생한 실시간 신호를 트랜잭션 커밋 이후에 전송한다.
 *
 * <p>커밋 전에 신호를 보내면 클라이언트가 즉시 재조회했을 때 변경 전 데이터를 볼 수 있다.
 * 트랜잭션이 없는 호출은 바로 실행한다.</p>
 */
@Component
public class AfterCommitExecutor {

    public void execute(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
