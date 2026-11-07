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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAccountSuccessTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_shouldSaveAccountSuccessfully() {
        when(userAccountRepository.existsByEmail("rahi@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password123!"))
                .thenReturn("encoded-password");

        when(userAccountRepository.saveAndFlush(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount result = accountService.createAccount(
                " Rahi Sadat ",
                " RAHI@example.com ",
                "Password123!",
                Role.STUDENT
        );

        assertEquals("Rahi Sadat", result.getFullName());
        assertEquals("rahi@example.com", result.getEmail());
        assertEquals("encoded-password", result.getPassword());
        assertEquals(Role.STUDENT, result.getRole());

        verify(userAccountRepository).saveAndFlush(any(UserAccount.class));
    }
}
