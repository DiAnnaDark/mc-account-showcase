package com.socialnetwork.mc_account.mapper;

import com.socialnetwork.mc_account.entity.Account;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.skillbox.socialnetwork.common.dto.AccountDto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountMapperTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("cc000000-0000-0000-0000-000000000001");

    private final AccountMapper accountMapper = Mappers.getMapper(AccountMapper.class);

    @Test
    void toDtoShouldMapAccountToDto() {
        Account account = Account.builder()
                .id(ACCOUNT_ID)
                .email("mapper@test.com")
                .firstName("Anna")
                .lastName("Test")
                .city("Moscow")
                .build();

        AccountDto result = accountMapper.toDto(account);

        assertEquals(ACCOUNT_ID, result.getId());
        assertEquals("mapper@test.com", result.getEmail());
        assertEquals("Anna", result.getFirstName());
        assertEquals("Test", result.getLastName());
        assertEquals("Moscow", result.getCity());
    }

    @Test
    void toEntityShouldMapDtoToAccount() {
        AccountDto dto = new AccountDto();
        dto.setId(ACCOUNT_ID);
        dto.setEmail("mapper@test.com");
        dto.setFirstName("Anna");
        dto.setLastName("Test");
        dto.setCity("Moscow");

        Account result = accountMapper.toEntity(dto);

        assertEquals(ACCOUNT_ID, result.getId());
        assertEquals("mapper@test.com", result.getEmail());
        assertEquals("Anna", result.getFirstName());
        assertEquals("Test", result.getLastName());
        assertEquals("Moscow", result.getCity());
    }

    @Test
    void toDtoListShouldMapAccountsToDtos() {
        Account account = Account.builder()
                .id(ACCOUNT_ID)
                .email("mapper@test.com")
                .firstName("Anna")
                .lastName("Test")
                .build();

        List<AccountDto> result = accountMapper.toDtoList(List.of(account));

        assertEquals(1, result.size());
        assertEquals(ACCOUNT_ID, result.getFirst().getId());
    }

    @Test
    void mapLocalDateTimeShouldReturnOffsetDateTimeWithUtc() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 25, 12, 0);

        OffsetDateTime result = accountMapper.map(dateTime);

        assertEquals(OffsetDateTime.of(dateTime, ZoneOffset.UTC), result);
    }

    @Test
    void mapOffsetDateTimeShouldReturnLocalDateTime() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2026, 5, 25, 12, 0, 0, 0, ZoneOffset.UTC);

        LocalDateTime result = accountMapper.map(offsetDateTime);

        assertEquals(LocalDateTime.of(2026, 5, 25, 12, 0), result);
    }

    @Test
    void mapNullDatesShouldReturnNull() {
        assertNull(accountMapper.map((LocalDateTime) null));
        assertNull(accountMapper.map((OffsetDateTime) null));
    }
}