package com.socialnetwork.mc_account.specifications;

import com.socialnetwork.mc_account.entity.Account;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountSpecificationsTest {

    @Test
    void idsInShouldReturnSpecification() {
        Specification<Account> specification =
                AccountSpecifications.idsIn(List.of(UUID.randomUUID()));

        assertNotNull(specification);
    }

    @Test
    void idsInShouldReturnNullPredicateForEmptyIds() {
        Root<Account> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Predicate predicate = AccountSpecifications.idsIn(List.of())
                .toPredicate(root, query, criteriaBuilder);

        assertNull(predicate);
    }

    @Test
    void authorContainsShouldReturnNullForBlankValue() {
        Root<Account> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Predicate predicate = AccountSpecifications.authorContains(" ")
                .toPredicate(root, query, criteriaBuilder);

        assertNull(predicate);
    }

    @Test
    void firstNameContainsShouldReturnSpecification() {
        Specification<Account> specification =
                AccountSpecifications.firstNameContains("anna");

        assertNotNull(specification);
    }

    @Test
    void cityContainsShouldReturnSpecification() {
        Specification<Account> specification =
                AccountSpecifications.cityContains("moscow");

        assertNotNull(specification);
    }

    @Test
    void hasBlockedStatusShouldReturnNullForNullValue() {
        Root<Account> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Predicate predicate = AccountSpecifications.hasBlockedStatus(null)
                .toPredicate(root, query, criteriaBuilder);

        assertNull(predicate);
    }

    @Test
    void hasDeletedStatusShouldReturnNullForNullValue() {
        Root<Account> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Predicate predicate = AccountSpecifications.hasDeletedStatus(null)
                .toPredicate(root, query, criteriaBuilder);

        assertNull(predicate);
    }

    @Test
    void birthDateFromShouldReturnSpecification() {
        Specification<Account> specification =
                AccountSpecifications.birthDateFrom(
                        OffsetDateTime.now(ZoneOffset.UTC)
                );

        assertNotNull(specification);
    }

    @Test
    void birthDateToShouldReturnSpecification() {
        Specification<Account> specification =
                AccountSpecifications.birthDateTo(
                        OffsetDateTime.now(ZoneOffset.UTC)
                );

        assertNotNull(specification);
    }

    @Test
    void ageFromShouldReturnSpecification() {
        Specification<Account> specification =
                AccountSpecifications.ageFrom(18);

        assertNotNull(specification);
    }

    @Test
    void ageToShouldReturnSpecification() {
        Specification<Account> specification =
                AccountSpecifications.ageTo(30);

        assertNotNull(specification);
    }

    @Test
    void ageFromShouldReturnNullPredicateForNullValue() {
        Root<Account> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Predicate predicate = AccountSpecifications.ageFrom(null)
                .toPredicate(root, query, criteriaBuilder);

        assertNull(predicate);
    }

    @Test
    void ageToShouldReturnNullPredicateForNullValue() {
        Root<Account> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Predicate predicate = AccountSpecifications.ageTo(null)
                .toPredicate(root, query, criteriaBuilder);

        assertNull(predicate);
    }
}
