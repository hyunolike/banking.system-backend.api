package com.banking_system.api_server.account.command.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByUserIdOrderByAccountNumberAsc(Long userId);

    /**
     * 잔액 변경 전용 조회. {@code SELECT ... FOR UPDATE} 로 행을 잠가
     * 동시에 들어온 출금/이체가 잔액을 덮어쓰지 못하게 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByIdForUpdate(@Param("accountNumber") Long accountNumber);
}
