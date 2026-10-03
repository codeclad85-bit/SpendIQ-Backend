package com.expense.expense_tracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.expense.expense_tracker.model.MonthlyLimit;

public interface MonthlyLimitRepository
        extends JpaRepository<MonthlyLimit, Long> {

    Optional<MonthlyLimit> findByUserId(Long userId);
}