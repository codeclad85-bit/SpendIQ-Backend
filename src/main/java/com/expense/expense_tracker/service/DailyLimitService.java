package com.expense.expense_tracker.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.expense.expense_tracker.model.DailyLimit;
import com.expense.expense_tracker.model.Expense;
import com.expense.expense_tracker.repository.DailyLimitRepository;
import com.expense.expense_tracker.repository.ExpenseRepository;

@Service
public class DailyLimitService {

    private final DailyLimitRepository dailyLimitRepository;
    private final ExpenseRepository expenseRepository;

    public DailyLimitService(
            DailyLimitRepository dailyLimitRepository,
            ExpenseRepository expenseRepository) {

        this.dailyLimitRepository = dailyLimitRepository;
        this.expenseRepository = expenseRepository;
    }

    public DailyLimit setDailyLimit(Long userId, double limitAmount) {

        DailyLimit dailyLimit =
                dailyLimitRepository.findByUserId(userId)
                        .orElse(new DailyLimit());

        dailyLimit.setUserId(userId);
        dailyLimit.setLimitAmount(limitAmount);

        return dailyLimitRepository.save(dailyLimit);
    }

    public DailyLimit getDailyLimit(Long userId) {

        return dailyLimitRepository
                .findByUserId(userId)
                .orElse(null);
    }

    public double getTodaySpending(Long userId) {

        LocalDate today = LocalDate.now();

        LocalDateTime start =
                today.atStartOfDay();

        LocalDateTime end =
                today.plusDays(1).atStartOfDay();

        List<Expense> expenses =
                expenseRepository.findByUserIdAndDateTimeBetween(
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