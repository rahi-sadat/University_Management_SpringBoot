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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteAccountTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    @Test
    void deleteAccount_shouldDeleteExistingStudent() {
        // Arrange: Create an existing student
        UserAccount student = new UserAccount();
        student.setFullName("Rahi Sadat");
        student.setEmail("rahi@example.com");
        student.setRole(Role.STUDENT);

        when(userAccountRepository.findById(2L))
                .thenReturn(Optional.of(student));

        // Act: Delete the student
        accountService.deleteAccount(2L, Role.STUDENT);

        // Assert: Verify Repository operations
        verify(userAccountRepository).findById(2L);
        verify(userAccountRepository).delete(student);
    }
}
