package com.banking_system.api_server.account.command.application;

import com.banking_system.api_server.account.command.domain.Account;
import com.banking_system.api_server.account.command.domain.AccountRepository;
import com.banking_system.api_server.account.command.domain.AccountTransaction;
import com.banking_system.api_server.account.command.domain.AccountTransactionRepository;
import com.banking_system.api_server.account.command.domain.TransactionType;
import com.banking_system.api_server.common.error.BusinessException;
import com.banking_system.api_server.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                          AccountTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountDtos.AccountResponse open(Long userId, AccountDtos.OpenAccountRequest request) {
        Account account = accountRepository.save(
                Account.open(userId, request.name(), request.initialBalance()));

        if (account.getBalance().signum() > 0) {
            transactionRepository.save(AccountTransaction.of(
                    account, TransactionType.DEPOSIT, account.getBalance(), null, "계좌 개설 최초 입금"));
        }
        log.info("account opened :: accountNumber={} userId={}", account.getAccountNumber(), userId);
        return toResponse(account);
    }

    public List<AccountDtos.AccountResponse> findMyAccounts(Long userId) {
        return accountRepository.findAllByUserIdOrderByAccountNumberAsc(userId).stream()
                .map(AccountService::toResponse)
                .toList();
    }

    public AccountDtos.AccountResponse findMyAccount(Long userId, Long accountNumber) {
        Account account = getAccount(accountNumber);
        account.requireOwner(userId);
        return toResponse(account);
    }

    @Transactional
    public AccountDtos.AccountResponse deposit(Long userId, Long accountNumber, AccountDtos.AmountRequest request) {
        Account account = getAccountForUpdate(accountNumber);
        account.requireOwner(userId);

        account.deposit(request.amount());
        transactionRepository.save(AccountTransaction.of(
                account, TransactionType.DEPOSIT, request.amount(), null, request.description()));
        return toResponse(account);
    }

    @Transactional
    public AccountDtos.AccountResponse withdraw(Long userId, Long accountNumber, AccountDtos.AmountRequest request) {
        Account account = getAccountForUpdate(accountNumber);
        account.requireOwner(userId);

        account.withdraw(request.amount());
        transactionRepository.save(AccountTransaction.of(
                account, TransactionType.WITHDRAW, request.amount(), null, request.description()));
        return toResponse(account);
    }

    /**
     * 계좌 이체.
     *
     * <p>두 계좌를 잠글 때는 항상 계좌번호가 작은 쪽부터 잠근다.
     * A→B 와 B→A 가 동시에 들어와도 락 획득 순서가 같아 데드락이 나지 않는다.</p>
     */
    @Transactional
    public AccountDtos.AccountResponse transfer(Long userId, Long fromAccountNumber, AccountDtos.TransferRequest request) {
        Long toAccountNumber = request.toAccountNumber();
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        List<Long> lockOrder = Stream.of(fromAccountNumber, toAccountNumber).sorted().toList();
        Account first = getAccountForUpdate(lockOrder.get(0));
        Account second = getAccountForUpdate(lockOrder.get(1));

        Account from = first.getAccountNumber().equals(fromAccountNumber) ? first : second;
        Account to = from == first ? second : first;

        from.requireOwner(userId);

        from.withdraw(request.amount());
        to.deposit(request.amount());

        transactionRepository.save(AccountTransaction.of(
                from, TransactionType.TRANSFER_OUT, request.amount(), to.getAccountNumber(), request.description()));
        transactionRepository.save(AccountTransaction.of(
                to, TransactionType.TRANSFER_IN, request.amount(), from.getAccountNumber(), request.description()));

        log.info("transfer done :: from={} to={} amount={}", fromAccountNumber, toAccountNumber, request.amount());
        return toResponse(from);
    }

    public List<AccountDtos.TransactionResponse> findTransactions(Long userId, Long accountNumber, Pageable pageable) {
        Account account = getAccount(accountNumber);
        account.requireOwner(userId);

        return transactionRepository.findByAccountNumberOrderByIdDesc(accountNumber, pageable)
                .map(tx -> new AccountDtos.TransactionResponse(
                        tx.getId(), tx.getType(), tx.getAmount(), tx.getBalanceAfter(),
                        tx.getCounterpartAccountNumber(), tx.getDescription(), tx.getCreatedAt()))
                .getContent();
    }

    private Account getAccount(Long accountNumber) {
        return accountRepository.findById(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private Account getAccountForUpdate(Long accountNumber) {
        return accountRepository.findByIdForUpdate(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private static AccountDtos.AccountResponse toResponse(Account account) {
        BigDecimal balance = account.getBalance();
        return new AccountDtos.AccountResponse(
                account.getAccountNumber(), account.getName(), balance, account.getCreatedAt());
    }
}
