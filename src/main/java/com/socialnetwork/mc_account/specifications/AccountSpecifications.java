package com.socialnetwork.mc_account.specifications;

import com.socialnetwork.mc_account.entity.Account;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AccountSpecifications {

    private static final String ID_FIELD = "id";
    private static final String FIRST_NAME_FIELD = "firstName";
    private static final String LAST_NAME_FIELD = "lastName";
    private static final String EMAIL_FIELD = "email";
    private static final String CITY_FIELD = "city";
    private static final String COUNTRY_FIELD = "country";
    private static final String IS_BLOCKED_FIELD = "isBlocked";
    private static final String IS_DELETED_FIELD = "isDeleted";
    private static final String BIRTH_DATE_FIELD = "birthDate";

    private static final String LIKE_WILDCARD = "%";
    private static final long AGE_BOUNDARY_SHIFT_YEARS = 1L;
    private static final long AGE_BOUNDARY_SHIFT_DAYS = 1L;

    private AccountSpecifications() {
    }

    public static Specification<Account> idsIn(List<UUID> ids) {
        return (root, query, criteriaBuilder) ->
                ids == null || ids.isEmpty() ? null : root.get(ID_FIELD).in(ids);
    }

    public static Specification<Account> authorContains(String author) {
        return (root, query, criteriaBuilder) -> {
            String pattern = toLikePattern(author);

            if (pattern == null) {
                return null;
            }

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(FIRST_NAME_FIELD)), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(LAST_NAME_FIELD)), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(EMAIL_FIELD)), pattern)
            );
        };
    }

    public static Specification<Account> firstNameContains(String firstName) {
        return containsIgnoreCase(FIRST_NAME_FIELD, firstName);
    }

    public static Specification<Account> lastNameContains(String lastName) {
        return containsIgnoreCase(LAST_NAME_FIELD, lastName);
    }

    public static Specification<Account> cityContains(String city) {
        return containsIgnoreCase(CITY_FIELD, city);
    }

    public static Specification<Account> countryContains(String country) {
        return containsIgnoreCase(COUNTRY_FIELD, country);
    }

    public static Specification<Account> hasBlockedStatus(Boolean isBlocked) {
        return (root, query, criteriaBuilder) ->
                isBlocked == null ? null : criteriaBuilder.equal(root.get(IS_BLOCKED_FIELD), isBlocked);
    }

    public static Specification<Account> hasDeletedStatus(Boolean isDeleted) {
        return (root, query, criteriaBuilder) ->
                isDeleted == null ? null : criteriaBuilder.equal(root.get(IS_DELETED_FIELD), isDeleted);
    }

    public static Specification<Account> birthDateFrom(OffsetDateTime from) {
        return (root, query, criteriaBuilder) ->
                from == null
                        ? null
                        : criteriaBuilder.greaterThanOrEqualTo(root.get(BIRTH_DATE_FIELD), from.toLocalDate());
    }

    public static Specification<Account> birthDateTo(OffsetDateTime to) {
        return (root, query, criteriaBuilder) ->
                to == null
                        ? null
                        : criteriaBuilder.lessThanOrEqualTo(root.get(BIRTH_DATE_FIELD), to.toLocalDate());
    }

    public static Specification<Account> ageFrom(Integer ageFrom) {
        return (root, query, criteriaBuilder) -> {
            if (ageFrom == null) {
                return null;
            }

            LocalDate maxBirthDate = LocalDate.now().minusYears(ageFrom);
            return criteriaBuilder.lessThanOrEqualTo(root.get(BIRTH_DATE_FIELD), maxBirthDate);
        };
    }

    public static Specification<Account> ageTo(Integer ageTo) {
        return (root, query, criteriaBuilder) -> {
            if (ageTo == null) {
                return null;
            }

            LocalDate minBirthDate = LocalDate.now()
                    .minusYears(ageTo + AGE_BOUNDARY_SHIFT_YEARS)
                    .plusDays(AGE_BOUNDARY_SHIFT_DAYS);

            return criteriaBuilder.greaterThanOrEqualTo(root.get(BIRTH_DATE_FIELD), minBirthDate);
        };
    }

    private static Specification<Account> containsIgnoreCase(String fieldName, String value) {
        return (root, query, criteriaBuilder) -> {
            String pattern = toLikePattern(value);

            return pattern == null
                    ? null
                    : criteriaBuilder.like(criteriaBuilder.lower(root.get(fieldName)), pattern);
        };
    }

    private static String toLikePattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return LIKE_WILDCARD + value.trim().toLowerCase() + LIKE_WILDCARD;
    }
}