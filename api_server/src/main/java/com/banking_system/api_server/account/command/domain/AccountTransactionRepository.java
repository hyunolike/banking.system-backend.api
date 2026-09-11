package com.banking_system.api_server.account.command.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    Page<AccountTransaction> findByAccountNumberOrderByIdDesc(Long accountNumber, Pageable pageable);
}
