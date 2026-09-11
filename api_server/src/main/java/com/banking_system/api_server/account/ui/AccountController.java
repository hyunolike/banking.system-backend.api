package com.banking_system.api_server.account.ui;

import com.banking_system.api_server.account.command.application.AccountDtos;
import com.banking_system.api_server.account.command.application.AccountService;
import com.banking_system.api_server.common.security.CurrentUser;
import com.banking_system.api_server.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountDtos.AccountResponse> open(@CurrentUser LoginUser loginUser,
                                                            @Valid @RequestBody AccountDtos.OpenAccountRequest request) {
        AccountDtos.AccountResponse response = accountService.open(loginUser.id(), request);
        return ResponseEntity.created(URI.create("/api/accounts/" + response.accountNumber())).body(response);
    }

    @GetMapping
    public List<AccountDtos.AccountResponse> myAccounts(@CurrentUser LoginUser loginUser) {
        return accountService.findMyAccounts(loginUser.id());
    }

    @GetMapping("/{accountNumber}")
    public AccountDtos.AccountResponse detail(@CurrentUser LoginUser loginUser,
                                              @PathVariable Long accountNumber) {
        return accountService.findMyAccount(loginUser.id(), accountNumber);
    }

    @PostMapping("/{accountNumber}/deposits")
    public AccountDtos.AccountResponse deposit(@CurrentUser LoginUser loginUser,
                                               @PathVariable Long accountNumber,
                                               @Valid @RequestBody AccountDtos.AmountRequest request) {
        return accountService.deposit(loginUser.id(), accountNumber, request);
    }

    @PostMapping("/{accountNumber}/withdrawals")
    public AccountDtos.AccountResponse withdraw(@CurrentUser LoginUser loginUser,
                                                @PathVariable Long accountNumber,
                                                @Valid @RequestBody AccountDtos.AmountRequest request) {
        return accountService.withdraw(loginUser.id(), accountNumber, request);
    }

    @PostMapping("/{accountNumber}/transfers")
    public AccountDtos.AccountResponse transfer(@CurrentUser LoginUser loginUser,
                                                @PathVariable Long accountNumber,
                                                @Valid @RequestBody AccountDtos.TransferRequest request) {
        return accountService.transfer(loginUser.id(), accountNumber, request);
    }

    @GetMapping("/{accountNumber}/transactions")
    public List<AccountDtos.TransactionResponse> transactions(@CurrentUser LoginUser loginUser,
                                                              @PathVariable Long accountNumber,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return accountService.findTransactions(loginUser.id(), accountNumber, pageable);
    }
}
