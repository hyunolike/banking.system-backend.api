package com.banking_system.api_server.account;

import com.banking_system.api_server.account.command.application.AccountDtos;
import com.banking_system.api_server.account.command.application.AccountService;
import com.banking_system.api_server.user.command.application.UserDtos;
import com.banking_system.api_server.user.command.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이체 동시성. 비관적 락({@code SELECT ... FOR UPDATE})이 실제로 갱신 손실을
 * 막는지, 그리고 반대 방향 이체가 섞여도 데드락으로 멈추지 않는지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AccountConcurrencyTest {

    private static final int THREADS = 8;
    private static final int TRANSFERS_PER_THREAD = 10;
    private static final BigDecimal AMOUNT = new BigDecimal("100");

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    private Long ownerId;
    private Long peerId;

    @BeforeEach
    void setUp() {
        ownerId = newUser();
        peerId = newUser();
    }

    private Long newUser() {
        return userService.signUp(new UserDtos.SignUpRequest(
                "테스터", UUID.randomUUID() + "@banking.test", "password1234")).id();
    }

    private AccountDtos.AccountResponse openAccount(Long userId, String balance) {
        return accountService.open(userId, new AccountDtos.OpenAccountRequest("테스트계좌", new BigDecimal(balance)));
    }

    /** 모든 작업을 동시에 출발시키고, 완료될 때까지 기다린 뒤 성공 건수를 돌려준다. */
    private int runConcurrently(List<Callable<Boolean>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Callable<Boolean> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    if (Boolean.TRUE.equals(task.call())) {
                        succeeded.incrementAndGet();
                    }
                    return null;
                }));
            }

            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
        return succeeded.get();
    }

    @Test
    @DisplayName("동시에 이체해도 잔액이 유실되지 않는다")
    void concurrentTransfersDoNotLoseMoney() throws Exception {
        BigDecimal initial = AMOUNT.multiply(BigDecimal.valueOf(THREADS * TRANSFERS_PER_THREAD));
        AccountDtos.AccountResponse from = openAccount(ownerId, initial.toPlainString());
        AccountDtos.AccountResponse to = openAccount(peerId, "0");

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            tasks.add(() -> {
                for (int i = 0; i < TRANSFERS_PER_THREAD; i++) {
                    accountService.transfer(ownerId, from.accountNumber(),
                            new AccountDtos.TransferRequest(to.accountNumber(), AMOUNT, "동시성"));
                }
                return true;
            });
        }

        assertThat(runConcurrently(tasks)).isEqualTo(THREADS);

        assertThat(accountService.findMyAccount(ownerId, from.accountNumber()).balance())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(accountService.findMyAccount(peerId, to.accountNumber()).balance())
                .isEqualByComparingTo(initial);
    }

    @Test
    @DisplayName("양방향 이체가 동시에 일어나도 데드락 없이 총액이 보존된다")
    void oppositeDirectionTransfersDoNotDeadlock() throws Exception {
        AccountDtos.AccountResponse a = openAccount(ownerId, "5000");
        AccountDtos.AccountResponse b = openAccount(peerId, "5000");
        BigDecimal total = new BigDecimal("10000");

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            boolean forward = t % 2 == 0;
            tasks.add(() -> {
                for (int i = 0; i < TRANSFERS_PER_THREAD; i++) {
                    if (forward) {
                        accountService.transfer(ownerId, a.accountNumber(),
                                new AccountDtos.TransferRequest(b.accountNumber(), AMOUNT, "A->B"));
                    } else {
                        accountService.transfer(peerId, b.accountNumber(),
                                new AccountDtos.TransferRequest(a.accountNumber(), AMOUNT, "B->A"));
                    }
                }
                return true;
            });
        }

        assertThat(runConcurrently(tasks)).isEqualTo(THREADS);

        BigDecimal balanceA = accountService.findMyAccount(ownerId, a.accountNumber()).balance();
        BigDecimal balanceB = accountService.findMyAccount(peerId, b.accountNumber()).balance();
        assertThat(balanceA.add(balanceB)).isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("잔액을 넘는 동시 출금은 성공한 건수만큼만 빠져나간다")
    void concurrentWithdrawalsNeverGoNegative() throws Exception {
        // 8건 중 3건만 성공할 수 있는 잔액
        AccountDtos.AccountResponse account = openAccount(ownerId, "300");

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            tasks.add(() -> {
                try {
                    accountService.withdraw(ownerId, account.accountNumber(),
                            new AccountDtos.AmountRequest(AMOUNT, "동시 출금"));
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            });
        }

        int succeeded = runConcurrently(tasks);

        assertThat(succeeded).isEqualTo(3);
        assertThat(accountService.findMyAccount(ownerId, account.accountNumber()).balance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
