package com.socialnetwork.mc_account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialnetwork.mc_account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.skillbox.socialnetwork.common.dto.AccountDto;
import ru.skillbox.socialnetwork.common.dto.PageAccountDto;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.socialnetwork.mc_account.service.AuthClient;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    private static final String BASE_URL = "/api/v1/account";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN = "Bearer test-token";

    private static final UUID ACCOUNT_ID = UUID.fromString("aa000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private AuthClient authClient;

    @Test
    void getCurrentAccountShouldReturnAccount() throws Exception {
        AccountDto accountDto = createAccountDto();

        when(accountService.getCurrentAccount(TOKEN)).thenReturn(accountDto);

        mockMvc.perform(get(BASE_URL + "/me")
                        .header(AUTHORIZATION_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.email").value("user@test.com"));

        verify(accountService).getCurrentAccount(TOKEN);
    }

    @Test
    void editCurrentAccountShouldReturnUpdatedAccount() throws Exception {
        AccountDto accountDto = createAccountDto();

        when(accountService.updateCurrentAccount(eq(TOKEN), any(AccountDto.class))).thenReturn(accountDto);

        mockMvc.perform(put(BASE_URL + "/me")
                        .header(AUTHORIZATION_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"));

        verify(accountService).updateCurrentAccount(eq(TOKEN), any(AccountDto.class));
    }

    @Test
    void deleteCurrentAccountShouldReturnMessage() throws Exception {
        when(accountService.deleteCurrentAccount(TOKEN)).thenReturn("Your account has been deleted");

        mockMvc.perform(delete(BASE_URL + "/me")
                        .header(AUTHORIZATION_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Your account has been deleted"));

        verify(accountService).deleteCurrentAccount(TOKEN);
    }

    @Test
    void blockAccountByIdShouldReturnMessage() throws Exception {
        when(accountService.blockAccountById(TOKEN, ACCOUNT_ID)).thenReturn("Account blocked: " + ACCOUNT_ID);

        mockMvc.perform(put(BASE_URL + "/block/{id}", ACCOUNT_ID)
                        .header(AUTHORIZATION_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Account blocked: " + ACCOUNT_ID));

        verify(accountService).blockAccountById(TOKEN, ACCOUNT_ID);
    }

    @Test
    void unblockAccountByIdShouldReturnMessage() throws Exception {
        when(accountService.unblockAccountById(TOKEN, ACCOUNT_ID)).thenReturn("Account unblocked: " + ACCOUNT_ID);

        mockMvc.perform(delete(BASE_URL + "/block/{id}", ACCOUNT_ID)
                        .header(AUTHORIZATION_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("Account unblocked: " + ACCOUNT_ID));

        verify(accountService).unblockAccountById(TOKEN, ACCOUNT_ID);
    }

    @Test
    void getAllAccountsShouldReturnPage() throws Exception {
        PageAccountDto pageDto = createPageAccountDto();

        when(accountService.getAllAccounts(0, 20)).thenReturn(pageDto);

        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("user@test.com"));

        verify(accountService).getAllAccounts(0, 20);
    }

    @Test
    void createAccountShouldReturnCreatedAccount() throws Exception {
        AccountDto accountDto = createAccountDto();

        when(accountService.createAccount(any(AccountDto.class))).thenReturn(accountDto);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"));

        verify(accountService).createAccount(any(AccountDto.class));
    }

    @Test
    void getAccountsByIdsShouldReturnAccounts() throws Exception {
        AccountDto accountDto = createAccountDto();

        when(accountService.getAccountsByIds(List.of(ACCOUNT_ID))).thenReturn(List.of(accountDto));

        mockMvc.perform(post(BASE_URL + "/find")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(ACCOUNT_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ACCOUNT_ID.toString()));

        verify(accountService).getAccountsByIds(List.of(ACCOUNT_ID));
    }

    @Test
    void searchAccountsShouldReturnPage() throws Exception {
        PageAccountDto pageDto = createPageAccountDto();

        when(accountService.searchAccounts("ivan", 0, 20)).thenReturn(pageDto);

        mockMvc.perform(get(BASE_URL + "/search")
                        .param("query", "ivan")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("user@test.com"));

        verify(accountService).searchAccounts("ivan", 0, 20);
    }

    @Test
    void getAccountsByCityShouldReturnAccounts() throws Exception {
        AccountDto accountDto = createAccountDto();

        when(accountService.getAccountsByCity("Moscow")).thenReturn(List.of(accountDto));

        mockMvc.perform(get(BASE_URL + "/by-city")
                        .param("city", "Moscow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("user@test.com"));

        verify(accountService).getAccountsByCity("Moscow");
    }

    @Test
    void getAccountsByAgeShouldReturnAccounts() throws Exception {
        AccountDto accountDto = createAccountDto();

        when(accountService.getAccountsByAge(18, 30)).thenReturn(List.of(accountDto));

        mockMvc.perform(get(BASE_URL + "/by-age")
                        .param("ageFrom", "18")
                        .param("ageTo", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("user@test.com"));

        verify(accountService).getAccountsByAge(18, 30);
    }

    @Test
    void getActiveAccountsShouldReturnAccounts() throws Exception {
        AccountDto accountDto = createAccountDto();

        when(accountService.getActiveAccounts()).thenReturn(List.of(accountDto));

        mockMvc.perform(get(BASE_URL + "/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("user@test.com"));

        verify(accountService).getActiveAccounts();
    }

    @Test
    void getAccountIdsShouldReturnIds() throws Exception {
        when(accountService.getAccountIds()).thenReturn(List.of(ACCOUNT_ID));

        mockMvc.perform(get(BASE_URL + "/accountIds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(ACCOUNT_ID.toString()));

        verify(accountService).getAccountIds();
    }

    @Test
    void getAccountByIdShouldReturnAccount() throws Exception {
        AccountDto accountDto = createAccountDto();

        when(accountService.getAccountById(ACCOUNT_ID)).thenReturn(accountDto);

        mockMvc.perform(get(BASE_URL + "/{id}", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID.toString()));

        verify(accountService).getAccountById(ACCOUNT_ID);
    }

    private AccountDto createAccountDto() {
        AccountDto accountDto = new AccountDto();
        accountDto.setId(ACCOUNT_ID);
        accountDto.setEmail("user@test.com");
        accountDto.setFirstName("Ivan");
        accountDto.setLastName("Ivanov");
        return accountDto;
    }

    private PageAccountDto createPageAccountDto() {
        PageAccountDto pageDto = new PageAccountDto();
        pageDto.setContent(List.of(createAccountDto()));
        pageDto.setTotalElements(1L);
        pageDto.setTotalPages(1);
        pageDto.setFirst(true);
        pageDto.setLast(true);
        pageDto.number(0);
        pageDto.size(20);
        return pageDto;
    }
}