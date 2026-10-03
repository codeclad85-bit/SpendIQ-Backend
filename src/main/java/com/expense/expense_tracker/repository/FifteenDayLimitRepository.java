package com.expense.expense_tracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.expense.expense_tracker.model.FifteenDayLimit;

public interface FifteenDayLimitRepository
        extends JpaRepository<FifteenDayLimit, Long> {

    Optional<FifteenDayLimit> findByUserId(Long userId);
}