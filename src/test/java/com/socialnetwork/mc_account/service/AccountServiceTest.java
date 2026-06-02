package com.socialnetwork.mc_account.service;

import com.socialnetwork.mc_account.entity.Account;
import com.socialnetwork.mc_account.kafka.dto.UserChangeEvent;
import com.socialnetwork.mc_account.kafka.dto.UserRegisteredEventDto;
import com.socialnetwork.mc_account.mapper.AccountMapper;
import com.socialnetwork.mc_account.repository.AccountRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.skillbox.socialnetwork.common.dto.AccountByFilterDto;
import ru.skillbox.socialnetwork.common.dto.AccountDto;
import ru.skillbox.socialnetwork.common.dto.PageAccountDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String AUTH_HEADER = "Bearer test-token";
    private static final UUID ACCOUNT_ID = UUID.fromString("aa000000-0000-0000-0000-000000000001");
    private static final String BLOCKED_METRIC_NAME = "account.blocked.by.admin.total";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AuthClient authClient;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private AccountService accountService;

    private Account account;
    private AccountDto accountDto;

    @BeforeEach
    void setUp() {
        account = createAccount();
        accountDto = createAccountDto();
    }

    @Test
    void getCurrentAccountShouldReturnAccountDto() {
        when(authClient.requireCurrentUserId(AUTH_HEADER)).thenReturn(ACCOUNT_ID);
        when(accountRepository.findByIdAndIsDeletedFalse(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        AccountDto result = accountService.getCurrentAccount(AUTH_HEADER);

        assertEquals(ACCOUNT_ID, result.getId());
        assertEquals("user@test.com", result.getEmail());

        verify(authClient).requireCurrentUserId(AUTH_HEADER);
        verify(accountRepository).findByIdAndIsDeletedFalse(ACCOUNT_ID);
        verify(accountMapper).toDto(account);
    }

    @Test
    void getCurrentAccountShouldThrowWhenAccountNotFound() {
        when(authClient.requireCurrentUserId(AUTH_HEADER)).thenReturn(ACCOUNT_ID);
        when(accountRepository.findByIdAndIsDeletedFalse(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> accountService.getCurrentAccount(AUTH_HEADER));
    }

    @Test
    void updateCurrentAccountShouldUpdateFieldsAndReturnDto() {
        AccountDto updateDto = createAccountDto();
        updateDto.setFirstName("Updated");
        updateDto.setCity("Moscow");

        when(authClient.requireCurrentUserId(AUTH_HEADER)).thenReturn(ACCOUNT_ID);
        when(accountRepository.findByIdAndIsDeletedFalse(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toDto(account)).thenReturn(updateDto);

        AccountDto result = accountService.updateCurrentAccount(AUTH_HEADER, updateDto);

        assertEquals("Updated", account.getFirstName());
        assertEquals("Moscow", account.getCity());
        assertEquals("Updated", result.getFirstName());

        verify(accountRepository).save(account);
    }

    @Test
    void deleteCurrentAccountShouldMarkAccountAsDeleted() {
        when(authClient.requireCurrentUserId(AUTH_HEADER)).thenReturn(ACCOUNT_ID);
        when(accountRepository.findByIdAndIsDeletedFalse(ACCOUNT_ID)).thenReturn(Optional.of(account));

        String result = accountService.deleteCurrentAccount(AUTH_HEADER);

        assertTrue(account.getIsDeleted());
        assertEquals("Your account has been deleted", result);

        verify(accountRepository).save(account);
    }

    @Test
    void blockAccountByIdShouldBlockAccountAndIncrementMetric() {
        when(accountRepository.findByIdAndIsDeletedFalse(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(meterRegistry.counter(BLOCKED_METRIC_NAME)).thenReturn(counter);

        String result = accountService.blockAccountById(AUTH_HEADER, ACCOUNT_ID);

        assertTrue(account.getIsBlocked());
        assertEquals("Account blocked: " + ACCOUNT_ID, result);

        verify(authClient).validateTokenOrThrow(AUTH_HEADER);
        verify(accountRepository).save(account);
        verify(counter).increment();
    }

    @Test
    void unblockAccountByIdShouldUnblockAccount() {
        account.setIsBlocked(true);

        when(accountRepository.findByIdAndIsDeletedFalse(ACCOUNT_ID)).thenReturn(Optional.of(account));

        String result = accountService.unblockAccountById(AUTH_HEADER, ACCOUNT_ID);

        assertFalse(account.getIsBlocked());
        assertEquals("Account unblocked: " + ACCOUNT_ID, result);

        verify(authClient).validateTokenOrThrow(AUTH_HEADER);
        verify(accountRepository).save(account);
    }

    @Test
    void getAllAccountsShouldReturnPageDto() {
        when(accountRepository.findByIsDeletedFalse(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(account), PageRequest.of(0, 20), 1));
        when(accountMapper.toDtoList(List.of(account))).thenReturn(List.of(accountDto));

        PageAccountDto result = accountService.getAllAccounts(0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
    }

    @Test
    void createAccountShouldInitializeAndSaveAccount() {
        when(accountMapper.toEntity(accountDto)).thenReturn(account);
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        AccountDto result = accountService.createAccount(accountDto);

        assertNotNull(account.getRegDate());
        assertFalse(account.getIsOnline());
        assertFalse(account.getIsBlocked());
        assertFalse(account.getIsDeleted());
        assertEquals(accountDto, result);

        verify(accountRepository).save(account);
    }

    @Test
    void handleUserRegisteredShouldSaveNewAccount() {
        UserRegisteredEventDto event = new UserRegisteredEventDto();
        event.setUserId(ACCOUNT_ID);
        event.setEmail("new@test.com");
        event.setFirstName("New");
        event.setLastName("User");

        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(false);

        accountService.handleUserRegistered(event);

        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void handleUserRegisteredShouldNotSaveExistingAccount() {
        UserRegisteredEventDto event = new UserRegisteredEventDto();
        event.setUserId(ACCOUNT_ID);

        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(true);

        accountService.handleUserRegistered(event);

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void handleUserRegisteredShouldThrowForInvalidEvent() {
        assertThrows(ResponseStatusException.class, () -> accountService.handleUserRegistered(null));
    }

    @Test
    void getAccountsByIdsShouldReturnEmptyListWhenIdsEmpty() {
        List<AccountDto> result = accountService.getAccountsByIds(List.of());

        assertTrue(result.isEmpty());
        verify(accountRepository, never()).findByIdInAndIsDeletedFalse(anyList());
    }

    @Test
    void getAccountsByIdsShouldReturnAccounts() {
        when(accountRepository.findByIdInAndIsDeletedFalse(List.of(ACCOUNT_ID))).thenReturn(List.of(account));
        when(accountMapper.toDtoList(List.of(account))).thenReturn(List.of(accountDto));

        List<AccountDto> result = accountService.getAccountsByIds(List.of(ACCOUNT_ID));

        assertEquals(1, result.size());
    }

    @Test
    void searchAccountsShouldReturnAllWhenQueryBlank() {
        when(accountRepository.findByIsDeletedFalse(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(account), PageRequest.of(0, 20), 1));
        when(accountMapper.toDtoList(List.of(account))).thenReturn(List.of(accountDto));

        PageAccountDto result = accountService.searchAccounts(" ", 0, 20);

        assertEquals(1, result.getContent().size());
        verify(accountRepository).findByIsDeletedFalse(PageRequest.of(0, 20));
        verify(accountRepository, never()).search(anyString(), any());
    }

    @Test
    void searchAccountsShouldUseSearchRepositoryWhenQueryPresent() {
        when(accountRepository.search("ivan", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(account), PageRequest.of(0, 20), 1));
        when(accountMapper.toDtoList(List.of(account))).thenReturn(List.of(accountDto));

        PageAccountDto result = accountService.searchAccounts(" ivan ", 0, 20);

        assertEquals(1, result.getContent().size());
        verify(accountRepository).search("ivan", PageRequest.of(0, 20));
    }

    @Test
    void getAccountsByCityShouldReturnEmptyListWhenCityBlank() {
        List<AccountDto> result = accountService.getAccountsByCity(" ");

        assertTrue(result.isEmpty());
        verify(accountRepository, never()).findByCityIgnoreCaseAndIsDeletedFalse(anyString());
    }

    @Test
    void getAccountsByAgeShouldThrowWhenRangeInvalid() {
        assertThrows(ResponseStatusException.class, () -> accountService.getAccountsByAge(40, 18));
    }

    @Test
    void getActiveAccountsShouldReturnAccounts() {
        when(accountRepository.findByIsOnlineTrueAndIsDeletedFalse()).thenReturn(List.of(account));
        when(accountMapper.toDtoList(List.of(account))).thenReturn(List.of(accountDto));

        List<AccountDto> result = accountService.getActiveAccounts();

        assertEquals(1, result.size());
    }

    @Test
    void getAccountIdsShouldReturnIds() {
        when(accountRepository.findAllActiveIds()).thenReturn(List.of(ACCOUNT_ID));

        List<UUID> result = accountService.getAccountIds();

        assertEquals(List.of(ACCOUNT_ID), result);
    }

    @Test
    void getAccountByIdShouldReturnAccount() {
        when(accountRepository.findByIdAndIsDeletedFalse(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        AccountDto result = accountService.getAccountById(ACCOUNT_ID);

        assertEquals(ACCOUNT_ID, result.getId());
    }

    private Account createAccount() {
        return Account.builder()
                .id(ACCOUNT_ID)
                .email("user@test.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .city("Saint Petersburg")
                .country("Russia")
                .birthDate(LocalDate.of(1990, 1, 1))
                .regDate(LocalDateTime.now())
                .createdOn(LocalDateTime.now())
                .lastOnlineTime(LocalDateTime.now())
                .isOnline(false)
                .isBlocked(false)
                .isDeleted(false)
                .build();
    }

    private AccountDto createAccountDto() {
        AccountDto dto = new AccountDto();
        dto.setId(ACCOUNT_ID);
        dto.setEmail("user@test.com");
        dto.setFirstName("Ivan");
        dto.setLastName("Ivanov");
        dto.setCity("Saint Petersburg");
        dto.setCountry("Russia");
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        return dto;
    }

    @Test
    void getAccountsByAgeShouldReturnAccounts() {
        when(accountRepository.findByBirthDateBetweenAndIsDeletedFalse(
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(account));

        when(accountMapper.toDtoList(List.of(account)))
                .thenReturn(List.of(accountDto));

        List<AccountDto> result =
                accountService.getAccountsByAge(18, 40);

        assertEquals(1, result.size());

        verify(accountRepository)
                .findByBirthDateBetweenAndIsDeletedFalse(
                        any(LocalDate.class),
                        any(LocalDate.class)
                );

        verify(accountMapper)
                .toDtoList(List.of(account));
    }

    @Test
    void handleUserChanged_shouldUpdateEmail() {

        UUID userId = UUID.randomUUID();

        UserChangeEvent event =
                new UserChangeEvent(
                        userId,
                        "new@mail.ru"
                );

        Account account = new Account();
        account.setId(userId);
        account.setEmail("old@mail.ru");

        when(accountRepository.findByIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(account));

        accountService.handleUserChanged(event);

        assertEquals("new@mail.ru", account.getEmail());

        verify(accountRepository).save(account);
    }

    @Test
    void handleUserChanged_shouldThrowWhenEventInvalid() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> accountService.handleUserChanged(null)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void searchAccountsByFilter_shouldReturnPageAccountDtoWhenFilterIsNull() {
        Page<Account> page =
                new PageImpl<>(List.of(account), PageRequest.of(0, 20), 1);

        when(accountRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(page);

        when(accountMapper.toDtoList(List.of(account)))
                .thenReturn(List.of(accountDto));

        PageAccountDto result =
                accountService.searchAccountsByFilter(null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        verify(accountRepository)
                .findAll(any(Specification.class), any(Pageable.class));

        verify(accountMapper)
                .toDtoList(List.of(account));
    }
}
