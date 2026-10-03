package com.expense.expense_tracker.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.expense.expense_tracker.model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdAndDateTimeBetween(
        Long userId,
        LocalDateTime start,
        LocalDateTime end
    );

    // Duplicate automatic transaction check
    boolean existsByTransactionRef(String transactionRef);
}