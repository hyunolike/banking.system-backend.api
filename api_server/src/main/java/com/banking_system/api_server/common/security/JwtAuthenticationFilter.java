package com.banking_system.api_server.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * {@code Authorization: Bearer <token>} 헤더를 읽어 SecurityContext 를 채운다.
 * 토큰이 없거나 유효하지 않으면 인증 없이 통과시키고, 접근 통제는 뒤쪽 인가 단계에 맡긴다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        resolveToken(request)
                .flatMap(tokenProvider::parse)
                .ifPresent(loginUser -> {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            loginUser, null, AuthorityUtils.createAuthorityList(SecurityRoles.ROLE_USER));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });

        filterChain.doFilter(request, response);
    }

    private java.util.Optional<String> resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return java.util.Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
    }
}
