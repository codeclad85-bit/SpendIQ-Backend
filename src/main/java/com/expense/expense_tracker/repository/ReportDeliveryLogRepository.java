package com.expense.expense_tracker.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.expense.expense_tracker.model.ReportDeliveryLog;

public interface ReportDeliveryLogRepository
        extends JpaRepository<ReportDeliveryLog, Long> {

    boolean existsByUserIdAndPeriodStartAndPeriodEnd(
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd);
}