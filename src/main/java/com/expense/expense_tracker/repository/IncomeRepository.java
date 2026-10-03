package com.expense.expense_tracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.expense.expense_tracker.model.Income;

public interface IncomeRepository
        extends JpaRepository<Income, Long> {

    Optional<Income> findByUserId(Long userId);
}