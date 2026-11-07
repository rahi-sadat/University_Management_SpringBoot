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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAccountTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    @Test
    void updateAccount_shouldUpdateDetailsAndEncodePassword() {
        // Arrange: Create an existing student
        UserAccount existing = new UserAccount();
        existing.setFullName("Old Name");
        existing.setEmail("old@example.com");
        existing.setPassword("old-encoded-password");
        existing.setRole(Role.STUDENT);

        when(userAccountRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(userAccountRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("NewPassword123!"))
                .thenReturn("new-encoded-password");

        when(userAccountRepository.saveAndFlush(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Update the account
        UserAccount result = accountService.updateAccount(
                1L,
                Role.STUDENT,
                " Updated Rahi ",
                " NEW@example.com ",
                "NewPassword123!"
        );

        // Assert: Check updated details
        assertEquals("Updated Rahi", result.getFullName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("new-encoded-password", result.getPassword());
        assertEquals(Role.STUDENT, result.getRole());

        verify(userAccountRepository).saveAndFlush(any(UserAccount.class));
        verify(passwordEncoder).encode("NewPassword123!");
    }
}
