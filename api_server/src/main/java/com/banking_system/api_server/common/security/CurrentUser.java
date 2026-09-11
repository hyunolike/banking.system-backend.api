package com.banking_system.api_server.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 컨트롤러 파라미터에 현재 로그인 사용자를 주입한다.
 *
 * <pre>{@code
 * @GetMapping("/api/accounts")
 * List<AccountSummary> myAccounts(@CurrentUser LoginUser user) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {
}
