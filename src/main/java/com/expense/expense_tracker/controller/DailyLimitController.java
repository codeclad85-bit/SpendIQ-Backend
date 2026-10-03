package com.expense.expense_tracker.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expense.expense_tracker.model.DailyLimit;
import com.expense.expense_tracker.service.DailyLimitService;

@RestController
@RequestMapping("/api/daily-limit")
public class DailyLimitController {

    private final DailyLimitService dailyLimitService;

    public DailyLimitController(DailyLimitService dailyLimitService) {
        this.dailyLimitService = dailyLimitService;
    }

    @PostMapping
    public DailyLimit setDailyLimit(
            @RequestBody DailyLimit dailyLimit) {

        return dailyLimitService.setDailyLimit(
                dailyLimit.getUserId(),
                dailyLimit.getLimitAmount()
        );
    }

    @GetMapping("/{userId}")
    public DailyLimit getDailyLimit(
            @PathVariable Long userId) {

        return dailyLimitService.getDailyLimit(userId);
    }

    @GetMapping("/{userId}/status")
    public Map<String, Object> getDailyLimitStatus(
            @PathVariable Long userId) {

        DailyLimit dailyLimit =
                dailyLimitService.getDailyLimit(userId);

        double spent =
                dailyLimitService.getTodaySpending(userId);

        Map<String, Object> response = new HashMap<>();

        if (dailyLimit == null) {
            response.put("message", "Daily limit not set");
            return response;
        }

        double limit = dailyLimit.getLimitAmount();

        double percentage = 0;

        if (limit > 0) {
            percentage = (spent / limit) * 100;
        }

        response.put("userId", userId);
        response.put("dailyLimit", limit);
        response.put("todaySpending", spent);
        response.put("percentage", percentage);
        response.put("exceededAmount",
                Math.max(0, spent - limit));

        if (spent >= limit) {
            response.put("status", "LIMIT_EXCEEDED");
        } else {
            response.put("status", "WITHIN_LIMIT");
        }

        return response;
    }
}