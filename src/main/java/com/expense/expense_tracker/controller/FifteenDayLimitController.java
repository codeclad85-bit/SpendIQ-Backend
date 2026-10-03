package com.expense.expense_tracker.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expense.expense_tracker.model.FifteenDayLimit;
import com.expense.expense_tracker.service.FifteenDayLimitService;

@RestController
@RequestMapping("/api/fifteen-day-limit")
public class FifteenDayLimitController {

    private final FifteenDayLimitService fifteenDayLimitService;

    public FifteenDayLimitController(
            FifteenDayLimitService fifteenDayLimitService) {

        this.fifteenDayLimitService = fifteenDayLimitService;
    }

    // Set 15 Days Limit
    @PostMapping
    public FifteenDayLimit setFifteenDayLimit(
            @RequestBody FifteenDayLimit fifteenDayLimit) {

        return fifteenDayLimitService.setFifteenDayLimit(
                fifteenDayLimit.getUserId(),
                fifteenDayLimit.getLimitAmount()
        );
    }

    // Get 15 Days Limit
    @GetMapping("/{userId}")
    public FifteenDayLimit getFifteenDayLimit(
            @PathVariable Long userId) {

        return fifteenDayLimitService
                .getFifteenDayLimit(userId);
    }

    // Get 15 Days Limit Status
    @GetMapping("/{userId}/status")
    public Map<String, Object> getFifteenDayLimitStatus(
            @PathVariable Long userId) {

        FifteenDayLimit fifteenDayLimit =
                fifteenDayLimitService
                        .getFifteenDayLimit(userId);

        double spent =
                fifteenDayLimitService
                        .getLastFifteenDaysSpending(userId);

        Map<String, Object> response =
                new HashMap<>();

        if (fifteenDayLimit == null) {
            response.put(
                    "message",
                    "15 Days limit not set"
            );

            return response;
        }

        double limit =
                fifteenDayLimit.getLimitAmount();

        double percentage = 0;

        if (limit > 0) {
            percentage =
                    (spent / limit) * 100;
        }

        response.put("userId", userId);
        response.put("fifteenDayLimit", limit);
        response.put("lastFifteenDaysSpending", spent);
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