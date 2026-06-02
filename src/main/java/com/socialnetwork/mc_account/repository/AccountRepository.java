package com.socialnetwork.mc_account.repository;

import com.socialnetwork.mc_account.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByIdAndIsDeletedFalse(UUID id);

    Page<Account> findByIsDeletedFalse(Pageable pageable);

    List<Account> findByIdInAndIsDeletedFalse(List<UUID> ids);

    List<Account> findByCityIgnoreCaseAndIsDeletedFalse(String city);

    List<Account> findByBirthDateBetweenAndIsDeletedFalse(LocalDate birthDateFrom, LocalDate birthDateTo);

    List<Account> findByIsOnlineTrueAndIsDeletedFalse();

    @Query("""
            select account.id
            from Account account
            where account.isDeleted = false
            """)
    List<UUID> findAllActiveIds();

    @Query("""
            select account
            from Account account
            where account.isDeleted = false
                and (
                    lower(account.firstName) like lower(concat('%', :query, '%'))
                    or lower(account.lastName) like lower(concat('%', :query, '%'))
                    or lower(account.email) like lower(concat('%', :query, '%'))
                    or lower(account.city) like lower(concat('%', :query, '%'))
                    or lower(account.country) like lower(concat('%', :query, '%'))
                )
            """)
    Page<Account> search(String query, Pageable pageable);
}