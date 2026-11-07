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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAccountsByRoleTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    @Test
    void findAccountsByRole_shouldReturnAccountsForRequestedRole() {
        // Arrange
        UserAccount student1 = new UserAccount();
        UserAccount student2 = new UserAccount();

        List<UserAccount> expectedStudents = List.of(student1, student2);

        when(userAccountRepository.findByRole(Role.STUDENT))
                .thenReturn(expectedStudents);

        // Act
        List<UserAccount> result =
                accountService.findAccountsByRole(Role.STUDENT);

        // Assert
        assertEquals(2, result.size());
        assertSame(expectedStudents, result);

        verify(userAccountRepository).findByRole(Role.STUDENT);
    }
}
