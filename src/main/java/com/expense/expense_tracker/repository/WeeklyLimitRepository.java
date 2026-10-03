package com.expense.expense_tracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.expense.expense_tracker.model.WeeklyLimit;

public interface WeeklyLimitRepository
        extends JpaRepository<WeeklyLimit, Long> {

    Optional<WeeklyLimit> findByUserId(Long userId);
}