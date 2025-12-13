package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findByUserIdAndMonth(UUID userId, String month);

    List<Budget> findByUserId(UUID userId);

    List<Budget> findByUserIdOrderByMonthDesc(UUID userId);

    boolean existsByUserIdAndMonth(UUID userId, String month);
}
