package com.socialnetwork.mc_account.mapper;

import com.socialnetwork.mc_account.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import ru.skillbox.socialnetwork.common.dto.AccountDto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface AccountMapper {

    ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.UTC;

    AccountDto toDto(Account account);

    Account toEntity(AccountDto dto);

    List<AccountDto> toDtoList(List<Account> accounts);

    default OffsetDateTime map(LocalDateTime value) {
        return value == null ? null : value.atOffset(DEFAULT_ZONE_OFFSET);
    }

    default LocalDateTime map(OffsetDateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }
}