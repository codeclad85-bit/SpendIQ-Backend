package com.expense.expense_tracker.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expense.expense_tracker.model.MonthlyLimit;
import com.expense.expense_tracker.service.MonthlyLimitService;

@RestController
@RequestMapping("/api/monthly-limit")
public class MonthlyLimitController {

    private final MonthlyLimitService monthlyLimitService;

    public MonthlyLimitController(
            MonthlyLimitService monthlyLimitService) {

        this.monthlyLimitService = monthlyLimitService;
    }

    // Set Monthly Limit
    @PostMapping
    public MonthlyLimit setMonthlyLimit(
            @RequestBody MonthlyLimit monthlyLimit) {

        return monthlyLimitService.setMonthlyLimit(
                monthlyLimit.getUserId(),
                monthlyLimit.getLimitAmount()
        );
    }

    // Get Monthly Limit
    @GetMapping("/{userId}")
    public MonthlyLimit getMonthlyLimit(
            @PathVariable Long userId) {

        return monthlyLimitService
                .getMonthlyLimit(userId);
    }

    // Get Monthly Limit Status
    @GetMapping("/{userId}/status")
    public Map<String, Object> getMonthlyLimitStatus(
            @PathVariable Long userId) {

        MonthlyLimit monthlyLimit =
                monthlyLimitService
                        .getMonthlyLimit(userId);

        double spent =
                monthlyLimitService
                        .getCurrentMonthSpending(userId);

        Map<String, Object> response =
                new HashMap<>();

        if (monthlyLimit == null) {
            response.put(
                    "message",
                    "Monthly limit not set"
            );

            return response;
        }

        double limit =
                monthlyLimit.getLimitAmount();

        double percentage = 0;

        if (limit > 0) {
            percentage =
                    (spent / limit) * 100;
        }

        response.put("userId", userId);
        response.put("monthlyLimit", limit);
        response.put("currentMonthSpending", spent);
        response.put("percentage", percentage);

        response.put(
                "exceededAmount",
                Math.max(0, spent - limit)
        );

        if (spent >= limit) {

            response.put(
                    "status",
                    "LIMIT_EXCEEDED"
            );

        } else {

            response.put(
                    "status",
                    "WITHIN_LIMIT"
            );
        }

        return response;
    }
}