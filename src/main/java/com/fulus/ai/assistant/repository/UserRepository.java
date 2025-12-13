package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    Optional<User> findByAccountNumber(String accountNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);
}
