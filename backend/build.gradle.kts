import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.authentication.http.HttpHeaderAuthentication

plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.coderhan.lastmission"
version = "0.0.1-SNAPSHOT"
description = "last-mission-backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()

    // 공인 TLS + Cloudflare Access로 보호되는 중앙 넥서스 레포지토리
    maven {
        url = uri("https://nexus.coder-han.com/repository/maven-snapshots/")

        val cloudflareAccessAuthorization = providers
            .environmentVariable("CF_ACCESS_AUTHORIZATION")
            .orNull

        if (!cloudflareAccessAuthorization.isNullOrBlank()) {
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = cloudflareAccessAuthorization
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:2.0.5")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // 채팅 실시간 수신 (STOMP over WebSocket, SockJS 미사용)
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // coder-han 중앙 인증 클라이언트
    implementation("com.coder-han:coder-han-auth-client:0.1.0-SNAPSHOT")

    // CoderHan Vault Cert 라이브러리 (운영 CockroachDB/Kafka 인증서 접속)
    implementation("com.coder-han:coder-han-vault-cert:0.5.19-SNAPSHOT")

    // Kafka
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    // DB 의존성 (CockroachDB / PostgreSQL 호환)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // 도메인 모듈 경계 및 구조 검증
    implementation("org.springframework.modulith:spring-modulith-starter-core")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
