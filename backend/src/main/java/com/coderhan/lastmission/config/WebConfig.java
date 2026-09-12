package com.coderhan.lastmission.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 프론트엔드가 백엔드와 다른 도메인(lastmission-api.example.com)에 있으므로
 * 크로스 오리진 요청을 허용한다.
 *
 * <p>두 도메인 모두 등록가능도메인이 example.com 이라 브라우저 기준 same-site 다.
 * 따라서 auth-core 세션 쿠키(Domain=example.com, SameSite=Lax)는 그대로 전달되지만,
 * 오리진이 다르므로 CORS 허용과 {@code allowCredentials} 는 반드시 필요하다.
 * 프론트도 fetch 에 {@code credentials: 'include'} 를 붙여야 쿠키가 실린다.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebConfig(
            @Value("${lastmission.cors.allowed-origins:"
                    + "https://lastmission.example.com,"
                    + "https://localhost.example.com:3000,"
                    + "http://localhost:3000}")
            String[] allowedOrigins
    ) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
