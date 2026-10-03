package com.expense.expense_tracker.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.expense.expense_tracker.model.BudgetPlanUpdate;
import com.expense.expense_tracker.repository.BudgetPlanUpdateRepository;

@Service
public class BudgetPlanUpdateService {

    private static final int MAX_UPDATES_PER_MONTH = 2;

    private final BudgetPlanUpdateRepository repository;

    public BudgetPlanUpdateService(
            BudgetPlanUpdateRepository repository) {
        this.repository = repository;
    }

    private BudgetPlanUpdate getCurrentRecord(Long userId) {

        LocalDate today = LocalDate.now();

        BudgetPlanUpdate record = repository
                .findByUserId(userId)
                .orElse(new BudgetPlanUpdate());

        if (record.getUserId() == null) {
            record.setUserId(userId);
            record.setUpdateCount(0);
            record.setUpdateMonth(today.getMonthValue());
            record.setUpdateYear(today.getYear());
        }

        // New month = automatically reset counter
        if (record.getUpdateMonth() != today.getMonthValue()
                || record.getUpdateYear() != today.getYear()) {

            record.setUpdateCount(0);
            record.setUpdateMonth(today.getMonthValue());
            record.setUpdateYear(today.getYear());
        }

        return record;
    }

    public boolean canUpdate(Long userId) {

        BudgetPlanUpdate record =
                getCurrentRecord(userId);

        return record.getUpdateCount()
                < MAX_UPDATES_PER_MONTH;
    }

    public void recordUpdate(Long userId) {

        BudgetPlanUpdate record =
                getCurrentRecord(userId);

        if (record.getUpdateCount()
                >= MAX_UPDATES_PER_MONTH) {

            throw new IllegalStateException(
                    "Monthly budget plan update limit reached."
            );
        }

        record.setUpdateCount(
                record.getUpdateCount() + 1
        );

        record.setLastUpdatedAt(
                LocalDateTime.now()
        );

        repository.save(record);
    }

    public int getRemainingUpdates(Long userId) {

        BudgetPlanUpdate record =
                getCurrentRecord(userId);

        return Math.max(
                0,
                MAX_UPDATES_PER_MONTH
                        - record.getUpdateCount()
        );
    }

    public int getUsedUpdates(Long userId) {

        return getCurrentRecord(userId)
                .getUpdateCount();
    }
}