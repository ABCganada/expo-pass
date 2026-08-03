package com.coderhan.lastmission.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * {@code example-vault-cert}가 자기 세션 갱신용 {@code TaskScheduler} 빈("coderhanVaultSessionTaskScheduler")을
 * 이미 등록해두고 있어서, Spring Boot의 자동 설정(TaskSchedulingAutoConfiguration)이
 * {@code @ConditionalOnMissingBean(TaskScheduler.class)}에 걸려 스케줄러를 만들지 않는다.
 * 그 결과 {@code @Scheduled} 메서드 전부가 컨텍스트에 남은 유일한 TaskScheduler 빈인 Vault의
 * 세션 갱신 스레드(풀 크기 1)를 그대로 같이 쓰게 된다.
 *
 * 그래서 이 앱 전용 TaskScheduler를 직접 등록한다 — 빈 이름을 "taskScheduler"로 둬야
 * {@code @EnableScheduling}이 여러 TaskScheduler 후보 중 이걸 우선 선택한다(Spring의 관례:
 * 후보가 여럿이면 이름이 "taskScheduler"인 빈을 찾는다). Vault 쪽은 항상
 * {@code @Qualifier("coderhanVaultSessionTaskScheduler")}로 자기 빈을 콕 집어 쓰므로 이 빈 추가와 무관하다.
 */
@Configuration
class SchedulingConfig {

    @Bean
    TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-task-");
        return scheduler;
    }
}
