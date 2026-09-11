package com.banking_system.api_server.user.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** 새 토큰을 발급하기 전, 해당 사용자의 기존 미사용 토큰을 모두 무효화한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now "
            + "where t.userId = :userId and t.usedAt is null")
    int invalidateAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
