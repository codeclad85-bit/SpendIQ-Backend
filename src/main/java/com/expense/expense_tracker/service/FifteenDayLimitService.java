package com.expense.expense_tracker.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.expense.expense_tracker.model.Expense;
import com.expense.expense_tracker.model.FifteenDayLimit;
import com.expense.expense_tracker.repository.ExpenseRepository;
import com.expense.expense_tracker.repository.FifteenDayLimitRepository;

@Service
public class FifteenDayLimitService {

    private final FifteenDayLimitRepository fifteenDayLimitRepository;
    private final ExpenseRepository expenseRepository;

    public FifteenDayLimitService(
            FifteenDayLimitRepository fifteenDayLimitRepository,
            ExpenseRepository expenseRepository) {

        this.fifteenDayLimitRepository = fifteenDayLimitRepository;
        this.expenseRepository = expenseRepository;
    }

    // Set 15 Days Limit
    public FifteenDayLimit setFifteenDayLimit(
            Long userId,
            double limitAmount) {

        FifteenDayLimit fifteenDayLimit =
                fifteenDayLimitRepository.findByUserId(userId)
                        .orElse(new FifteenDayLimit());

        fifteenDayLimit.setUserId(userId);
        fifteenDayLimit.setLimitAmount(limitAmount);

        return fifteenDayLimitRepository.save(fifteenDayLimit);
    }

    // Get 15 Days Limit
    public FifteenDayLimit getFifteenDayLimit(Long userId) {

        return fifteenDayLimitRepository
                .findByUserId(userId)
                .orElse(null);
    }

    // Calculate Last 15 Days Spending
    public double getLastFifteenDaysSpending(Long userId) {

        LocalDateTime end = LocalDateTime.now();

        LocalDateTime start = end.minusDays(15);

        List<Expense> expenses =
                expenseRepository
                        .findByUserIdAndDateTimeBetween(
                                userId,
                                start,
                                end
                        );

        double total = 0;

        for (Expense expense : expenses) {
            total += expense.getAmount();
        }

        return total;
    }
}