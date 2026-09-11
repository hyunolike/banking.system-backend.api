package com.banking_system.api_server.stats.ui;

import com.banking_system.api_server.stats.query.StatsQueryService;
import com.banking_system.api_server.stats.query.SystemSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스케줄러 서버 전용 엔드포인트. {@code X-Internal-Api-Key} 헤더로만 접근할 수 있다.
 *
 * <p>예전에는 스케줄러가 Spring Data REST 가 자동 생성한 {@code /api/userDatas} 를
 * 인증 없이 긁어가고 있었다. 사용자 목록 전체 대신 집계값만 노출한다.</p>
 */
@RestController
@RequestMapping("/api/internal")
public class InternalStatsController {

    private final StatsQueryService statsQueryService;

    public InternalStatsController(StatsQueryService statsQueryService) {
        this.statsQueryService = statsQueryService;
    }

    @GetMapping("/stats/snapshot")
    public SystemSnapshot snapshot() {
        return statsQueryService.snapshot();
    }
}
