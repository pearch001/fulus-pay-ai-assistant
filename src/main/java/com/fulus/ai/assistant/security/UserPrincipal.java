package com.fulus.ai.assistant.security;

import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Custom UserDetails implementation for Spring Security
 */
@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private UUID id;
    private String phoneNumber;
    private String name;
    private String password; // used by Spring Security for authentication
    private String pin; // optional numeric PIN used for certain operations
    private boolean active;
    private boolean accountLocked;
    private UserRole role; // User role

    /**
     * Create UserPrincipal from User entity
     */
    public static UserPrincipal create(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getPhoneNumber(),
                user.getName(),
                user.getPassword(),
                user.getPin(),
                user.isActive(),
                user.isAccountLocked(),
                user.getRole() != null ? user.getRole() : UserRole.USER
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Return role-based authority
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // Use phone number as username
        return phoneNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
