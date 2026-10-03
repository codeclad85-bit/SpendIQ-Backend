package com.expense.expense_tracker.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.expense.expense_tracker.model.Expense;
import com.expense.expense_tracker.model.MonthlyLimit;
import com.expense.expense_tracker.repository.ExpenseRepository;
import com.expense.expense_tracker.repository.MonthlyLimitRepository;

@Service
public class MonthlyLimitService {

    private final MonthlyLimitRepository monthlyLimitRepository;
    private final ExpenseRepository expenseRepository;

    public MonthlyLimitService(
            MonthlyLimitRepository monthlyLimitRepository,
            ExpenseRepository expenseRepository) {

        this.monthlyLimitRepository = monthlyLimitRepository;
        this.expenseRepository = expenseRepository;
    }

    // Set Monthly Limit
    public MonthlyLimit setMonthlyLimit(
            Long userId,
            double limitAmount) {

        MonthlyLimit monthlyLimit =
                monthlyLimitRepository.findByUserId(userId)
                        .orElse(new MonthlyLimit());

        monthlyLimit.setUserId(userId);
        monthlyLimit.setLimitAmount(limitAmount);

        return monthlyLimitRepository.save(monthlyLimit);
    }

    // Get Monthly Limit
    public MonthlyLimit getMonthlyLimit(Long userId) {

        return monthlyLimitRepository
                .findByUserId(userId)
                .orElse(null);
    }

    // Calculate Current Month Spending
    public double getCurrentMonthSpending(Long userId) {

        LocalDate today = LocalDate.now();

        // First day of current month
        LocalDate startOfMonth =
                today.withDayOfMonth(1);

        // First day of next month
        LocalDate startOfNextMonth =
                startOfMonth.plusMonths(1);

        LocalDateTime start =
                startOfMonth.atStartOfDay();

        LocalDateTime end =
                startOfNextMonth.atStartOfDay();

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