package com.expense.expense_tracker.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.expense.expense_tracker.model.Expense;
import com.expense.expense_tracker.model.WeeklyLimit;
import com.expense.expense_tracker.repository.ExpenseRepository;
import com.expense.expense_tracker.repository.WeeklyLimitRepository;

@Service
public class WeeklyLimitService {

    private final WeeklyLimitRepository weeklyLimitRepository;
    private final ExpenseRepository expenseRepository;

    public WeeklyLimitService(
            WeeklyLimitRepository weeklyLimitRepository,
            ExpenseRepository expenseRepository) {

        this.weeklyLimitRepository = weeklyLimitRepository;
        this.expenseRepository = expenseRepository;
    }

    // Set Weekly Limit
    public WeeklyLimit setWeeklyLimit(
            Long userId,
            double limitAmount) {

        WeeklyLimit weeklyLimit =
                weeklyLimitRepository.findByUserId(userId)
                        .orElse(new WeeklyLimit());

        weeklyLimit.setUserId(userId);
        weeklyLimit.setLimitAmount(limitAmount);

        return weeklyLimitRepository.save(weeklyLimit);
    }

    // Get Weekly Limit
    public WeeklyLimit getWeeklyLimit(Long userId) {

        return weeklyLimitRepository
                .findByUserId(userId)
                .orElse(null);
    }

    // Calculate This Week's Spending
    public double getThisWeekSpending(Long userId) {

        LocalDate today = LocalDate.now();

        // Monday of current week
        LocalDate startOfWeek =
                today.with(DayOfWeek.MONDAY);

        // Next Monday
        LocalDate endOfWeek =
                startOfWeek.plusDays(7);

        LocalDateTime start =
                startOfWeek.atStartOfDay();

        LocalDateTime end =
                endOfWeek.atStartOfDay();

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