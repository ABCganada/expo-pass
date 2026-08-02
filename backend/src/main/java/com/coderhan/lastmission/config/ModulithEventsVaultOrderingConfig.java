package com.coderhan.lastmission.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Modulith JDBC 이벤트 모듈의 {@code databaseType} 빈은 DB dialect를 알아내려고
 * 기동 시점에 곧바로 JDBC 커넥션을 연다. 근데 이 프로젝트는 Vault로부터 DB용 mTLS
 * 인증서를 받아와야 실제 커넥션이 가능한 구조라, 두 빈의 생성 순서가 우연에 맡겨지면
 * Vault 인증서 발급보다 databaseType 생성이 먼저 실행되면서 "SSL bundle has not been
 * initialized" 오류로 기동이 실패한다.
 *
 * 그래서 {@link BeanDefinition#setDependsOn(String...)}으로 두 빈의
 * 생성 순서를 명시적으로 강제한다
 */
@Configuration
public class ModulithEventsVaultOrderingConfig {

    private static final Logger log = LoggerFactory.getLogger(ModulithEventsVaultOrderingConfig.class);

    private static final String MODULITH_DATABASE_TYPE_BEAN = "databaseType";
    private static final String VAULT_CERTIFICATE_MANAGER_BEAN = "vaultCertificateManager";

    @Bean
    static BeanFactoryPostProcessor orderModulithDatabaseTypeAfterVaultCertificateManager() {
        return ModulithEventsVaultOrderingConfig::applyDependsOn;
    }

    private static void applyDependsOn(ConfigurableListableBeanFactory beanFactory) {
        boolean hasDatabaseType = beanFactory.containsBeanDefinition(MODULITH_DATABASE_TYPE_BEAN);
        boolean hasVaultCertificateManager = beanFactory.containsBeanDefinition(VAULT_CERTIFICATE_MANAGER_BEAN);

        if (!hasDatabaseType || !hasVaultCertificateManager) {
            log.debug("[ModulithEventsVaultOrderingConfig] databaseType={}, vaultCertificateManager={} — "
                    + "둘 다 있을 때만 순서를 강제한다. 순서 강제를 건너뛴다.", hasDatabaseType, hasVaultCertificateManager);
            return;
        }

        BeanDefinition databaseTypeDefinition = beanFactory.getBeanDefinition(MODULITH_DATABASE_TYPE_BEAN);
        databaseTypeDefinition.setDependsOn(VAULT_CERTIFICATE_MANAGER_BEAN);

        log.info("[ModulithEventsVaultOrderingConfig] Modulith '{}' 빈이 Vault SSL 번들 발급('{}') 이후에 "
                + "생성되도록 순서를 강제했습니다.", MODULITH_DATABASE_TYPE_BEAN, VAULT_CERTIFICATE_MANAGER_BEAN);
    }
}
