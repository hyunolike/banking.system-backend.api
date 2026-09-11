package com.banking_system.api_server.stats.query;

import com.banking_system.api_server.account.command.domain.Account;
import com.banking_system.api_server.account.command.domain.AccountRepository;
import com.banking_system.api_server.user.command.domain.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 읽기 전용 조회 모델. 커맨드 측 엔티티를 재사용하되 밖으로는 DTO 만 내보낸다.
 */
@Service
@Transactional(readOnly = true)
public class StatsQueryService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final EntityManager entityManager;

    public StatsQueryService(UserRepository userRepository,
                             AccountRepository accountRepository,
                             EntityManager entityManager) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.entityManager = entityManager;
    }

    public SystemSnapshot snapshot() {
        BigDecimal totalBalance = entityManager
                .createQuery("select coalesce(sum(a.balance), 0) from Account a", BigDecimal.class)
                .getSingleResult()
                .setScale(Account.BALANCE_SCALE, java.math.RoundingMode.HALF_UP);

        return new SystemSnapshot(
                userRepository.count(),
                accountRepository.count(),
                totalBalance,
                LocalDateTime.now());
    }
}
