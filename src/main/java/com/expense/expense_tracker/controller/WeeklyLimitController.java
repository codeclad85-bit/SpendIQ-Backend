package com.expense.expense_tracker.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expense.expense_tracker.model.WeeklyLimit;
import com.expense.expense_tracker.service.WeeklyLimitService;

@RestController
@RequestMapping("/api/weekly-limit")
public class WeeklyLimitController {

    private final WeeklyLimitService weeklyLimitService;

    public WeeklyLimitController(
            WeeklyLimitService weeklyLimitService) {
        this.weeklyLimitService = weeklyLimitService;
    }

    // Set Weekly Limit
    @PostMapping
    public WeeklyLimit setWeeklyLimit(
            @RequestBody WeeklyLimit weeklyLimit) {

        return weeklyLimitService.setWeeklyLimit(
                weeklyLimit.getUserId(),
                weeklyLimit.getLimitAmount()
        );
    }

    // Get Weekly Limit
    @GetMapping("/{userId}")
    public WeeklyLimit getWeeklyLimit(
            @PathVariable Long userId) {

        return weeklyLimitService.getWeeklyLimit(userId);
    }

    // Get Weekly Limit Status
    @GetMapping("/{userId}/status")
    public Map<String, Object> getWeeklyLimitStatus(
            @PathVariable Long userId) {

        WeeklyLimit weeklyLimit =
                weeklyLimitService.getWeeklyLimit(userId);

        double spent =
                weeklyLimitService.getThisWeekSpending(userId);

        Map<String, Object> response =
                new HashMap<>();

        if (weeklyLimit == null) {
            response.put(
                    "message",
                    "Weekly limit not set"
            );

            return response;
        }

        double limit =
                weeklyLimit.getLimitAmount();

        double percentage = 0;

        if (limit > 0) {
            percentage =
                    (spent / limit) * 100;
        }

        response.put("userId", userId);
        response.put("weeklyLimit", limit);
        response.put("thisWeekSpending", spent);
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