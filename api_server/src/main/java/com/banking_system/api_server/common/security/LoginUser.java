package com.banking_system.api_server.common.security;

/**
 * 인증된 사용자. {@code SecurityContext} 의 principal 로 사용된다.
 */
public record LoginUser(Long id, String email) {
}
