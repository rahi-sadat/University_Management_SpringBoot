package com.example.university_management.service;

import com.example.university_management.model.Role;
import com.example.university_management.model.UserAccount;
import com.example.university_management.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAccountDuplicateEmailTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_shouldRejectDuplicateEmail() {
        when(userAccountRepository.existsByEmail("rahi@example.com"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accountService.createAccount(
                        "Rahi Sadat",
                        "rahi@example.com",
                        "Password123!",
                        Role.STUDENT
                )
        );

        assertEquals(409, exception.getStatusCode().value());

        verify(userAccountRepository, never())
                .saveAndFlush(any(UserAccount.class));

        verifyNoInteractions(passwordEncoder);
    }
}
