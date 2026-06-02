package com.socialnetwork.mc_account.controller;

import com.socialnetwork.mc_account.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.skillbox.socialnetwork.common.dto.AccountByFilterDto;
import ru.skillbox.socialnetwork.common.dto.AccountDto;
import ru.skillbox.socialnetwork.common.dto.PageAccountDto;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "20";
    private static final int MIN_PAGE = 0;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final AccountService accountService;

    @GetMapping("/me")
    public AccountDto getCurrentAccount(@RequestHeader(AUTHORIZATION_HEADER) String authHeader) {
        return accountService.getCurrentAccount(authHeader);
    }

    @PutMapping("/me")
    public AccountDto editCurrentAccount(
            @RequestHeader(AUTHORIZATION_HEADER) String authHeader,
            @Valid @RequestBody AccountDto accountDto
    ) {
        return accountService.updateCurrentAccount(authHeader, accountDto);
    }

    @DeleteMapping("/me")
    public String deleteCurrentAccount(@RequestHeader(AUTHORIZATION_HEADER) String authHeader) {
        return accountService.deleteCurrentAccount(authHeader);
    }

    @PutMapping("/block/{id}")
    public String blockAccountById(
            @RequestHeader(AUTHORIZATION_HEADER) String authHeader,
            @PathVariable UUID id
    ) {
        return accountService.blockAccountById(authHeader, id);
    }

    @DeleteMapping("/block/{id}")
    public String unblockAccountById(
            @RequestHeader(AUTHORIZATION_HEADER) String authHeader,
            @PathVariable UUID id
    ) {
        return accountService.unblockAccountById(authHeader, id);
    }

    @GetMapping
    public PageAccountDto getAllAccounts(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(MIN_PAGE) Integer page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(MIN_SIZE) @Max(MAX_SIZE) Integer size
    ) {
        return accountService.getAllAccounts(page, size);
    }

    @PostMapping
    public AccountDto createAccount(@Valid @RequestBody AccountDto accountDto) {
        return accountService.createAccount(accountDto);
    }

    @PostMapping("/searchByFilter")
    public PageAccountDto searchAccountsByFilter(@Valid @RequestBody AccountByFilterDto filterDto) {
        return accountService.searchAccountsByFilter(filterDto);
    }

    @PostMapping("/find")
    public List<AccountDto> getAccountsByIds(@RequestBody @NotEmpty List<UUID> ids) {
        return accountService.getAccountsByIds(ids);
    }

    @GetMapping("/search")
    public PageAccountDto searchAccounts(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(MIN_PAGE) Integer page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(MIN_SIZE) @Max(MAX_SIZE) Integer size
    ) {
        return accountService.searchAccounts(query, page, size);
    }

    @GetMapping("/by-city")
    public List<AccountDto> getAccountsByCity(@RequestParam String city) {
        return accountService.getAccountsByCity(city);
    }

    @GetMapping("/by-age")
    public List<AccountDto> getAccountsByAge(
            @RequestParam @Min(0) Integer ageFrom,
            @RequestParam @Min(0) Integer ageTo
    ) {
        return accountService.getAccountsByAge(ageFrom, ageTo);
    }

    @GetMapping("/active")
    public List<AccountDto> getActiveAccounts() {
        return accountService.getActiveAccounts();
    }

    @GetMapping("/accountIds")
    public List<UUID> getAccountIds() {
        return accountService.getAccountIds();
    }

    @GetMapping("/{id}")
    public AccountDto getAccountById(@PathVariable UUID id) {
        return accountService.getAccountById(id);
    }
}