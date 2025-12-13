package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, UUID> {

    List<SavingsAccount> findByUserId(UUID userId);

    List<SavingsAccount> findByUserIdAndMaturityDateAfter(UUID userId, LocalDate date);

    @Query("SELECT s FROM SavingsAccount s WHERE s.maturityDate <= :date")
    List<SavingsAccount> findMaturedAccounts(@Param("date") LocalDate date);

    @Query("SELECT s FROM SavingsAccount s WHERE s.userId = :userId AND s.currentAmount < s.targetAmount")
    List<SavingsAccount> findActiveAccountsByUser(@Param("userId") UUID userId);
}
