package com.banking_system.api_server.common.security;

public final class SecurityRoles {

    /** 로그인한 일반 사용자 */
    public static final String ROLE_USER = "ROLE_USER";

    /** 스케줄러 등 내부 서비스 호출자 */
    public static final String ROLE_INTERNAL = "ROLE_INTERNAL";

    private SecurityRoles() {
    }
}
