
package com.example.university_management.service;

import com.example.university_management.model.Role;
import com.example.university_management.model.UserAccount;
import com.example.university_management.repository.UserAccountRepository;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

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

                       //Reject duplicate email
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



        //Find accounts by role
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

                        //Reject Invalid Account
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
           // Update Account


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



    //Delete Account


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
