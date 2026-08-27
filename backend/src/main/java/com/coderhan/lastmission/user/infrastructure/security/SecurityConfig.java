package com.coderhan.lastmission.user.infrastructure.security;

import com.coderhan.lastmission.user.application.UserProvisioningService;
import com.coderhan.auth.client.AuthClient;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthClient authClient,
            UserProvisioningService userProvisioningService,
            @Value("${lastmission.security.csrf.secure-cookie:true}") boolean secureCsrfCookie) {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieName("LASTMISSION-XSRF-TOKEN");
        csrfTokenRepository.setHeaderName("X-LASTMISSION-XSRF-TOKEN");
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.path("/").secure(secureCsrfCookie).sameSite("Lax"));

        // 요청 범위 저장소. LastMissionAuthenticationFilter 가 여기에 저장하고 SessionManagementFilter 가
        // 여기서 containsContext 를 확인한다 — 둘이 같은 인스턴스여야 하므로 필터에도 이 인스턴스를 넘긴다.
        // 요청 한 건만 살고 버려지므로 STATELESS 를 유지한다(세션 저장이 아니다).
        SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();

        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        // 광고 노출/클릭 집계는 공개 트래킹 엔드포인트라 CSRF 제외
                        // 토스 웹훅도 외부 서버가 직접 호출하는 콜백이라 CSRF 토큰을 보낼 수 없음
                        .ignoringRequestMatchers("/api/v1/banners/*/impressions", "/api/v1/banners/*/clicks",
                                "/webhooks/payments/toss"))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(RequestCacheConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/check").denyAll()
                        .requestMatchers("/auth/**", "/error").permitAll()
                        // 토스 웹훅 수신 — 인증 없이 열려있음. 발신 IP 검증은 별도 작업(TODO)
                        .requestMatchers("/webhooks/payments/toss").permitAll()
                        // 활성 배너 목록 및 집계는 비인증 허용
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/banners/*/impressions", "/api/v1/banners/*/clicks").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/manager/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/kafka/test/**").hasAnyRole("DEVELOPER")
                        .requestMatchers("/api/**").hasAnyRole("USER")
                        // WebSocket 핸드셰이크. CSRF 대상이 아니므로 허용 Origin 검사가 별도로 필요하다
                        // (StompConfig 의 setAllowedOrigins 참고).
                        .requestMatchers("/ws/**").hasAnyRole("USER")
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((_, response, _) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((_, response, _) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .addFilterBefore(new LastMissionAuthenticationFilter(authClient, userProvisioningService,
                                securityContextRepository),
                        AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
