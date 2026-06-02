package com.socialnetwork.mc_account.repository;

import com.socialnetwork.mc_account.PostgresTestContainer;
import com.socialnetwork.mc_account.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest extends PostgresTestContainer {

    private static final UUID ACTIVE_ACCOUNT_ID = UUID.fromString("bb000000-0000-0000-0000-000000000001");
    private static final UUID DELETED_ACCOUNT_ID = UUID.fromString("bb000000-0000-0000-0000-000000000002");

    @Autowired
    private AccountRepository accountRepository;

    private Account activeAccount;
    private Account deletedAccount;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();

        activeAccount = createAccount(
                ACTIVE_ACCOUNT_ID,
                "active@test.com",
                "Ivan",
                "Petrov",
                "Moscow",
                LocalDate.of(1990, 1, 1),
                true,
                false
        );

        deletedAccount = createAccount(
                DELETED_ACCOUNT_ID,
                "deleted@test.com",
                "Deleted",
                "User",
                "Kazan",
                LocalDate.of(1985, 5, 10),
                false,
                true
        );

        accountRepository.saveAll(List.of(activeAccount, deletedAccount));
    }

    @Test
    void findByIdAndIsDeletedFalseShouldReturnOnlyActiveAccount() {
        Optional<Account> result = accountRepository.findByIdAndIsDeletedFalse(ACTIVE_ACCOUNT_ID);
        Optional<Account> deletedResult = accountRepository.findByIdAndIsDeletedFalse(DELETED_ACCOUNT_ID);

        assertTrue(result.isPresent());
        assertTrue(deletedResult.isEmpty());
    }

    @Test
    void findByIsDeletedFalseShouldReturnOnlyActiveAccounts() {
        Page<Account> result = accountRepository.findByIsDeletedFalse(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(ACTIVE_ACCOUNT_ID, result.getContent().getFirst().getId());
    }

    @Test
    void findByIdInAndIsDeletedFalseShouldIgnoreDeletedAccounts() {
        List<Account> result = accountRepository.findByIdInAndIsDeletedFalse(
                List.of(ACTIVE_ACCOUNT_ID, DELETED_ACCOUNT_ID)
        );

        assertEquals(1, result.size());
        assertEquals(ACTIVE_ACCOUNT_ID, result.getFirst().getId());
    }

    @Test
    void findByCityIgnoreCaseAndIsDeletedFalseShouldFindByCityIgnoringCase() {
        List<Account> result = accountRepository.findByCityIgnoreCaseAndIsDeletedFalse("moscow");

        assertEquals(1, result.size());
        assertEquals(ACTIVE_ACCOUNT_ID, result.getFirst().getId());
    }

    @Test
    void findByBirthDateBetweenAndIsDeletedFalseShouldFindByBirthDateRange() {
        List<Account> result = accountRepository.findByBirthDateBetweenAndIsDeletedFalse(
                LocalDate.of(1989, 1, 1),
                LocalDate.of(1991, 12, 31)
        );

        assertEquals(1, result.size());
        assertEquals(ACTIVE_ACCOUNT_ID, result.getFirst().getId());
    }

    @Test
    void findByIsOnlineTrueAndIsDeletedFalseShouldReturnOnlineActiveAccounts() {
        List<Account> result = accountRepository.findByIsOnlineTrueAndIsDeletedFalse();

        assertEquals(1, result.size());
        assertEquals(ACTIVE_ACCOUNT_ID, result.getFirst().getId());
    }

    @Test
    void findAllActiveIdsShouldReturnOnlyNotDeletedIds() {
        List<UUID> result = accountRepository.findAllActiveIds();

        assertEquals(List.of(ACTIVE_ACCOUNT_ID), result);
    }

    @Test
    void searchShouldFindByFirstName() {
        Page<Account> result = accountRepository.search("ivan", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(ACTIVE_ACCOUNT_ID, result.getContent().getFirst().getId());
    }

    @Test
    void searchShouldFindByEmail() {
        Page<Account> result = accountRepository.search("active@test.com", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(ACTIVE_ACCOUNT_ID, result.getContent().getFirst().getId());
    }

    @Test
    void searchShouldNotReturnDeletedAccounts() {
        Page<Account> result = accountRepository.search("deleted", PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
    }

    private Account createAccount(
            UUID id,
            String email,
            String firstName,
            String lastName,
            String city,
            LocalDate birthDate,
            boolean isOnline,
            boolean isDeleted
    ) {
        LocalDateTime now = LocalDateTime.now();

        return Account.builder()
                .id(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .city(city)
                .country("Russia")
                .birthDate(birthDate)
                .regDate(now)
                .lastOnlineTime(now)
                .isOnline(isOnline)
                .isBlocked(false)
                .isDeleted(isDeleted)
                .createdOn(now)
                .build();
    }
}