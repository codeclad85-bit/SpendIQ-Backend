package com.expense.expense_tracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.expense.expense_tracker.model.BudgetPlanUpdate;

public interface BudgetPlanUpdateRepository
        extends JpaRepository<BudgetPlanUpdate, Long> {

    Optional<BudgetPlanUpdate> findByUserId(Long userId);
}