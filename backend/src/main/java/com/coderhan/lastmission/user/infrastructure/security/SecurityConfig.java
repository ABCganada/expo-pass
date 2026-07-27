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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthClient authClient,
            UserProvisioningService userProvisioningService,
            @Value("${lastmission.security.csrf.secure-cookie:true}") boolean secureCsrfCookie) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieName("LASTMISSION-XSRF-TOKEN");
        csrfTokenRepository.setHeaderName("X-LASTMISSION-XSRF-TOKEN");
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.path("/").secure(secureCsrfCookie).sameSite("Lax"));

        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(requestCache -> requestCache.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/check").denyAll()
                        .requestMatchers("/auth/**", "/error").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/kafka/test/**").hasAnyRole("ADMIN", "DEVELOPER")
                        .requestMatchers("/api/**").hasAnyRole("ADMIN", "MANAGER", "USER", "DEVELOPER")
                        // WebSocket 핸드셰이크. CSRF 대상이 아니므로 허용 Origin 검사가 별도로 필요하다
                        // (StompConfig 의 setAllowedOrigins 참고).
                        .requestMatchers("/ws/**").hasAnyRole("ADMIN", "MANAGER", "USER", "DEVELOPER")
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .addFilterBefore(new LastMissionAuthenticationFilter(authClient, userProvisioningService),
                        AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
