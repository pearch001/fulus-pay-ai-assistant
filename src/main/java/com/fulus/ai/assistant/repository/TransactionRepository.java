package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.Transaction;
import com.fulus.ai.assistant.enums.TransactionCategory;
import com.fulus.ai.assistant.enums.TransactionStatus;
import com.fulus.ai.assistant.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReference(String reference);

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Transaction> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    List<Transaction> findByUserIdAndCategory(UUID userId, TransactionCategory category);

    List<Transaction> findByUserIdAndType(UUID userId, TransactionType type);

    List<Transaction> findByUserIdAndStatus(UUID userId, TransactionStatus status);

    Integer countByUserIdAndStatus(UUID userId, TransactionStatus status);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.type = :type AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumAmountByUserAndTypeAndDateRange(
        @Param("userId") UUID userId,
        @Param("type") TransactionType type,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :start AND :end GROUP BY t.category")
    List<Object[]> findCategorySpendingByUserAndDateRange(
        @Param("userId") UUID userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Find transactions with filtering and pagination
     */
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
           "AND (:category IS NULL OR t.category = :category) " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt <= :toDate) " +
           "AND t.status = 'COMPLETED' " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findFilteredTransactions(
        @Param("userId") UUID userId,
        @Param("category") TransactionCategory category,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );

    /**
     * Count transactions by type in date range
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.type = :type AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :start AND :end")
    Long countByUserAndTypeAndDateRange(
        @Param("userId") UUID userId,
        @Param("type") TransactionType type,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Find transactions for export (no pagination)
     */
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
           "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAt <= :toDate) " +
           "AND t.status = 'COMPLETED' " +
           "ORDER BY t.createdAt ASC")
    List<Transaction> findTransactionsForExport(
        @Param("userId") UUID userId,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate
    );

    /**
     * Get category spending with transaction count
     */
    @Query("SELECT t.category, SUM(t.amount), COUNT(t) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.type = 'DEBIT' AND t.status = 'COMPLETED' " +
           "AND t.createdAt BETWEEN :start AND :end " +
           "GROUP BY t.category " +
           "ORDER BY SUM(t.amount) DESC")
    List<Object[]> findCategorySpendingWithCount(
        @Param("userId") UUID userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
