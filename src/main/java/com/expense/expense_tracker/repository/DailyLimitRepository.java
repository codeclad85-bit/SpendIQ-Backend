package com.expense.expense_tracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.expense.expense_tracker.model.DailyLimit;

public interface DailyLimitRepository extends JpaRepository<DailyLimit, Long> {

    Optional<DailyLimit> findByUserId(Long userId);
}