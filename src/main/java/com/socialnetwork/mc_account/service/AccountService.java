package com.socialnetwork.mc_account.service;

import com.socialnetwork.mc_account.entity.Account;
import com.socialnetwork.mc_account.kafka.dto.UserChangeEvent;
import com.socialnetwork.mc_account.kafka.dto.UserRegisteredEventDto;
import com.socialnetwork.mc_account.mapper.AccountMapper;
import com.socialnetwork.mc_account.repository.AccountRepository;
import com.socialnetwork.mc_account.specifications.AccountSpecifications;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.skillbox.socialnetwork.common.dto.AccountByFilterDto;
import ru.skillbox.socialnetwork.common.dto.AccountDto;
import ru.skillbox.socialnetwork.common.dto.AccountSearchDto;
import ru.skillbox.socialnetwork.common.dto.PageAccountDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final long AGE_SHIFT_FOR_BIRTHDAY_BOUNDARY = 1L;

    private static final String ACCOUNT_NOT_FOUND_MESSAGE = "Account not found: ";
    private static final String ACCOUNT_NOT_FOUND_WITH_ID_MESSAGE = "Account not found with id: ";
    private static final String INVALID_AGE_RANGE_MESSAGE = "Invalid age range";
    private static final String INVALID_USER_REGISTERED_EVENT_MESSAGE = "Invalid user registered event";

    private static final String ACCOUNT_DELETED_MESSAGE = "Your account has been deleted";
    private static final String ACCOUNT_BLOCKED_MESSAGE = "Account blocked: ";
    private static final String ACCOUNT_UNBLOCKED_MESSAGE = "Account unblocked: ";

    private static final String ACCOUNT_BLOCKED_BY_ADMIN_METRIC = "account.blocked.by.admin.total";

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AuthClient authClient;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    public AccountDto getCurrentAccount(String authHeader) {
        UUID currentUserId = authClient.requireCurrentUserId(authHeader);
        Account account = getActiveAccountOrThrow(currentUserId);

        return accountMapper.toDto(account);
    }

    @Transactional
    public AccountDto updateCurrentAccount(String authHeader, AccountDto accountDto) {
        UUID currentUserId = authClient.requireCurrentUserId(authHeader);
        Account account = getActiveAccountOrThrow(currentUserId);

        updateAccountFields(account, accountDto);

        Account savedAccount = accountRepository.save(account);
        return accountMapper.toDto(savedAccount);
    }

    @Transactional
    public String deleteCurrentAccount(String authHeader) {
        UUID currentUserId = authClient.requireCurrentUserId(authHeader);
        Account account = getActiveAccountOrThrow(currentUserId);

        account.setIsDeleted(true);
        accountRepository.save(account);

        return ACCOUNT_DELETED_MESSAGE;
    }

    @Transactional
    public String blockAccountById(String authHeader, UUID id) {
        authClient.validateTokenOrThrow(authHeader);

        Account account = getActiveAccountOrThrow(id);
        account.setIsBlocked(true);
        accountRepository.save(account);

        meterRegistry.counter(ACCOUNT_BLOCKED_BY_ADMIN_METRIC).increment();

        return ACCOUNT_BLOCKED_MESSAGE + id;
    }

    @Transactional
    public String unblockAccountById(String authHeader, UUID id) {
        authClient.validateTokenOrThrow(authHeader);

        Account account = getActiveAccountOrThrow(id);
        account.setIsBlocked(false);
        accountRepository.save(account);

        return ACCOUNT_UNBLOCKED_MESSAGE + id;
    }

    @Transactional(readOnly = true)
    public PageAccountDto getAllAccounts(Integer pageNumber, Integer pageSize) {
        Pageable pageable = createPageRequest(pageNumber, pageSize);
        Page<Account> accountPage = accountRepository.findByIsDeletedFalse(pageable);

        return toPageAccountDto(accountPage);
    }

    @Transactional
    public AccountDto createAccount(AccountDto accountDto) {
        Account account = accountMapper.toEntity(accountDto);
        initializeNewAccount(account);

        Account savedAccount = accountRepository.save(account);
        return accountMapper.toDto(savedAccount);
    }

    @Transactional
    public void handleUserRegistered(UserRegisteredEventDto event) {
        validateUserRegisteredEvent(event);

        if (accountRepository.existsById(event.getUserId())) {
            return;
        }

        Account account = createAccountFromUserRegisteredEvent(event);
        accountRepository.save(account);
    }

    @Transactional
    public void handleUserChanged(UserChangeEvent event) {
        validateUserChangeEvent(event);

        Account account = getActiveAccountOrThrow(event.userId());
        account.setEmail(event.email());

        accountRepository.save(account);
    }

    private void validateUserChangeEvent(UserChangeEvent event) {
        if (event == null || event.userId() == null || event.email() == null || event.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user change event");
        }
    }

    @Transactional(readOnly = true)
    public PageAccountDto searchAccountsByFilter(AccountByFilterDto filterDto) {
        int pageNumber = getPageNumber(filterDto);
        int pageSize = getPageSize(filterDto);

        Pageable pageable = createPageRequest(pageNumber, pageSize);
        Specification<Account> specification = createAccountSpecification(filterDto);

        Page<Account> accountPage = accountRepository.findAll(specification, pageable);
        return toPageAccountDto(accountPage);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Account> accounts = accountRepository.findByIdInAndIsDeletedFalse(ids);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public PageAccountDto searchAccounts(String query, Integer pageNumber, Integer pageSize) {
        Pageable pageable = createPageRequest(pageNumber, pageSize);

        if (query == null || query.isBlank()) {
            Page<Account> accountPage = accountRepository.findByIsDeletedFalse(pageable);
            return toPageAccountDto(accountPage);
        }

        Page<Account> accountPage = accountRepository.search(query.trim(), pageable);
        return toPageAccountDto(accountPage);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByCity(String city) {
        if (city == null || city.isBlank()) {
            return List.of();
        }

        List<Account> accounts = accountRepository.findByCityIgnoreCaseAndIsDeletedFalse(city.trim());
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByAge(Integer ageFrom, Integer ageTo) {
        validateAgeRange(ageFrom, ageTo);

        LocalDate birthDateTo = LocalDate.now().minusYears(ageFrom);
        LocalDate birthDateFrom = LocalDate.now()
                .minusYears(ageTo + AGE_SHIFT_FOR_BIRTHDAY_BOUNDARY)
                .plusDays(AGE_SHIFT_FOR_BIRTHDAY_BOUNDARY);

        List<Account> accounts = accountRepository.findByBirthDateBetweenAndIsDeletedFalse(birthDateFrom, birthDateTo);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getActiveAccounts() {
        List<Account> accounts = accountRepository.findByIsOnlineTrueAndIsDeletedFalse();
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<UUID> getAccountIds() {
        return accountRepository.findAllActiveIds();
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountById(UUID id) {
        Account account = getActiveAccountOrThrow(id);
        return accountMapper.toDto(account);
    }

    private Account getActiveAccountOrThrow(UUID id) {
        return accountRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        ACCOUNT_NOT_FOUND_MESSAGE + id));
    }

    private void updateAccountFields(Account account, AccountDto accountDto) {
        account.setPhone(accountDto.getPhone());
        account.setPhoto(accountDto.getPhoto());
        account.setAbout(accountDto.getAbout());
        account.setCity(accountDto.getCity());
        account.setCountry(accountDto.getCountry());
        account.setFirstName(accountDto.getFirstName());
        account.setLastName(accountDto.getLastName());
        account.setBirthDate(accountDto.getBirthDate());
        account.setPhotoName(accountDto.getPhotoName());
        account.setEmojiStatus(accountDto.getEmojiStatus());
    }

    private void initializeNewAccount(Account account) {
        LocalDateTime now = LocalDateTime.now();

        account.setRegDate(now);
        account.setCreatedOn(now);
        account.setLastOnlineTime(now);
        account.setIsOnline(false);
        account.setIsBlocked(false);
        account.setIsDeleted(false);
    }

    private void validateUserRegisteredEvent(UserRegisteredEventDto event) {
        if (event == null || event.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_USER_REGISTERED_EVENT_MESSAGE);
        }
    }

    private Account createAccountFromUserRegisteredEvent(UserRegisteredEventDto event) {
        Account account = new Account();

        account.setId(event.getUserId());
        account.setEmail(event.getEmail());
        account.setFirstName(event.getFirstName());
        account.setLastName(event.getLastName());

        initializeNewAccount(account);

        return account;
    }

    private Specification<Account> createAccountSpecification(AccountByFilterDto filterDto) {
        AccountSearchDto search = filterDto == null ? null : filterDto.getAccountSearchDto();

        if (search == null) {
            return Specification.where(AccountSpecifications.hasDeletedStatus(false));
        }

        Specification<Account> specification = Specification
                .where(AccountSpecifications.idsIn(search.getIds()))
                .and(AccountSpecifications.authorContains(search.getAuthor()))
                .and(AccountSpecifications.firstNameContains(search.getFirstName()))
                .and(AccountSpecifications.lastNameContains(search.getLastName()))
                .and(AccountSpecifications.cityContains(search.getCity()))
                .and(AccountSpecifications.countryContains(search.getCountry()))
                .and(AccountSpecifications.hasBlockedStatus(search.getIsBlocked()))
                .and(AccountSpecifications.birthDateFrom(search.getBirthDateFrom()))
                .and(AccountSpecifications.birthDateTo(search.getBirthDateTo()))
                .and(AccountSpecifications.ageFrom(search.getAgeFrom()))
                .and(AccountSpecifications.ageTo(search.getAgeTo()));

        Boolean isDeleted = search.getIsDeleted();
        return specification.and(AccountSpecifications.hasDeletedStatus(isDeleted != null ? isDeleted : false));
    }

    private Pageable createPageRequest(Integer pageNumber, Integer pageSize) {
        int safePageNumber = pageNumber == null ? DEFAULT_PAGE_NUMBER : pageNumber;
        int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;

        return PageRequest.of(safePageNumber, safePageSize);
    }

    private int getPageNumber(AccountByFilterDto filterDto) {
        return filterDto == null || filterDto.getPageNumber() == null
                ? DEFAULT_PAGE_NUMBER
                : filterDto.getPageNumber();
    }

    private int getPageSize(AccountByFilterDto filterDto) {
        return filterDto == null || filterDto.getPageSize() == null
                ? DEFAULT_PAGE_SIZE
                : filterDto.getPageSize();
    }

    private void validateAgeRange(Integer ageFrom, Integer ageTo) {
        if (ageFrom == null || ageTo == null || ageFrom < 0 || ageTo < 0 || ageFrom > ageTo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_AGE_RANGE_MESSAGE);
        }
    }

    private PageAccountDto toPageAccountDto(Page<Account> accountPage) {
        PageAccountDto pageDto = new PageAccountDto();

        pageDto.setContent(accountMapper.toDtoList(accountPage.getContent()));
        pageDto.setTotalElements(accountPage.getTotalElements());
        pageDto.setTotalPages(accountPage.getTotalPages());
        pageDto.setFirst(accountPage.isFirst());
        pageDto.setLast(accountPage.isLast());
        pageDto.number(accountPage.getNumber());
        pageDto.size(accountPage.getSize());

        return pageDto;
    }
}