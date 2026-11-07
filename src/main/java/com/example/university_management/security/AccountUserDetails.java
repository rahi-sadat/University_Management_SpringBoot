package com.example.university_management.security;

import com.example.university_management.model.UserAccount;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

/**
 * Custom Spring Security UserDetails adapter.
 * Wraps our UserAccount entity so Spring Security can verify the password,
 * assign granted authorities ("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_STUDENT"),
 * and provide the account ID.
 */
public class AccountUserDetails extends User {

    private final Long accountId;

    public AccountUserDetails(UserAccount account) {
        // Spring Security expects roles with the "ROLE_" prefix
        super(
                account.getEmail(),
                account.getPassword(),
                AuthorityUtils.createAuthorityList("ROLE_" + account.getRole().name())
        );
        this.accountId = account.getId();
    }

    public Long getAccountId() {
        return accountId;
    }
}
