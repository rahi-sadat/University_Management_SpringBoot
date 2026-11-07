package com.example.university_management.service;

import com.example.university_management.model.Role;
import com.example.university_management.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FindAccountByIdInvalidIdTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    @Test
    void findAccountByIdAndRole_shouldRejectInvalidId() {
        // Arrange
        Long invalidId = 0L;

        // Act and Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.findAccountByIdAndRole(
                        invalidId, Role.STUDENT)
        );

        // Verify exception message
        assertEquals(
                "Account ID must be positive.",
                exception.getMessage()
        );

        // Verify Repository was never called
        verifyNoInteractions(userAccountRepository);
    }
}
