package com.banking_system.api_server.stats.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 스케줄러가 주기적으로 수집해 가는 시스템 현황 스냅샷.
 *
 * @param userCount     가입자 수
 * @param accountCount  개설된 계좌 수
 * @param totalBalance  전체 계좌 잔액 합계
 * @param generatedAt   스냅샷 생성 시각
 */
public record SystemSnapshot(long userCount,
                             long accountCount,
                             BigDecimal totalBalance,
                             LocalDateTime generatedAt) {
}
