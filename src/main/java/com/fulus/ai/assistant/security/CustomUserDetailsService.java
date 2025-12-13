package com.fulus.ai.assistant.security;

import com.fulus.ai.assistant.entity.User;
import com.fulus.ai.assistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Custom UserDetailsService implementation for Spring Security
 *
 * Loads user-specific data for authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        log.debug("Loading user by phone number: {}", phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with phone number: " + phoneNumber));

        return UserPrincipal.create(user);
    }

    /**
     * Load user by ID
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID userId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with ID: " + userId));

        return UserPrincipal.create(user);
    }
}
