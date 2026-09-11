package com.banking_system.api_server.common.config;

import com.banking_system.api_server.common.security.CorsProperties;
import com.banking_system.api_server.common.security.InternalApiKeyFilter;
import com.banking_system.api_server.common.security.InternalApiProperties;
import com.banking_system.api_server.common.security.JwtAuthenticationFilter;
import com.banking_system.api_server.common.security.JwtProperties;
import com.banking_system.api_server.common.security.JwtTokenProvider;
import com.banking_system.api_server.common.security.RestAccessDeniedHandler;
import com.banking_system.api_server.common.security.RestAuthenticationEntryPoint;
import com.banking_system.api_server.common.security.SecurityPolicyProperties;
import com.banking_system.api_server.common.security.SecurityRoles;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 접근 통제 정책.
 *
 * <ul>
 *   <li>{@code /api/auth/**} : 회원가입·로그인 (비인증 허용)</li>
 *   <li>{@code /api/internal/**} : 내부 서비스 전용 (X-Internal-Api-Key)</li>
 *   <li>그 외 전부 : Bearer 토큰 필요</li>
 * </ul>
 *
 * 이전에는 Spring Data REST 가 모든 테이블에 인증 없는 CRUD 를 열어두고 있었다.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, InternalApiProperties.class,
        SecurityPolicyProperties.class, CorsProperties.class})
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtTokenProvider tokenProvider,
                                                   InternalApiProperties internalApiProperties,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                // 토큰 기반 stateless API 이므로 CSRF 토큰과 세션을 사용하지 않는다.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/signup", "/api/auth/login", "/api/auth/password-reset").permitAll()
                        .requestMatchers("/api/internal/**").hasAuthority(SecurityRoles.ROLE_INTERNAL)
                        .anyRequest().hasAuthority(SecurityRoles.ROLE_USER))
                .addFilterBefore(new InternalApiKeyFilter(internalApiProperties.apiKey()),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 허용 오리진을 지정하지 않으면 아무 오리진도 등록하지 않아 교차 출처 요청이 차단된다.
     * 토큰은 Authorization 헤더로 전달하므로 쿠키 자격증명(allowCredentials)은 켜지 않는다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (!properties.enabled()) {
            return source;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(properties.allowedMethods());
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(properties.maxAge());

        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
