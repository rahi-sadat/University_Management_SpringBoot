package com.example.university_management.service;

import com.example.university_management.model.UserAccount;
import com.example.university_management.repository.UserAccountRepository;
import com.example.university_management.security.AccountUserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Spring Security UserDetailsService implementation.
 * Loads a user's account from the database by email for authentication and token validation.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public AccountUserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        UserAccount account = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Account not found for email: " + normalizedEmail));

        return new AccountUserDetails(account);
    }
}
