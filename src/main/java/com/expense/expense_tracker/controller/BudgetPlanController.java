package com.expense.expense_tracker.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expense.expense_tracker.model.Income;
import com.expense.expense_tracker.repository.IncomeRepository;
import com.expense.expense_tracker.service.BudgetPlanUpdateService;
import com.expense.expense_tracker.service.DailyLimitService;
import com.expense.expense_tracker.service.FifteenDayLimitService;
import com.expense.expense_tracker.service.MonthlyLimitService;
import com.expense.expense_tracker.service.WeeklyLimitService;

@RestController
@RequestMapping("/api/budget-plan")
public class BudgetPlanController {

    private final BudgetPlanUpdateService updateService;
    private final IncomeRepository incomeRepository;
    private final DailyLimitService dailyLimitService;
    private final WeeklyLimitService weeklyLimitService;
    private final FifteenDayLimitService fifteenDayLimitService;
    private final MonthlyLimitService monthlyLimitService;

    public BudgetPlanController(
            BudgetPlanUpdateService updateService,
            IncomeRepository incomeRepository,
            DailyLimitService dailyLimitService,
            WeeklyLimitService weeklyLimitService,
            FifteenDayLimitService fifteenDayLimitService,
            MonthlyLimitService monthlyLimitService) {

        this.updateService = updateService;
        this.incomeRepository = incomeRepository;
        this.dailyLimitService = dailyLimitService;
        this.weeklyLimitService = weeklyLimitService;
        this.fifteenDayLimitService = fifteenDayLimitService;
        this.monthlyLimitService = monthlyLimitService;
    }

    @PostMapping
    public ResponseEntity<?> saveBudgetPlan(
            @RequestBody Map<String, Object> request) {

        try {
            Long userId = Long.valueOf(
                    request.get("userId").toString()
            );

            double monthlyIncome = Double.parseDouble(
                    request.get("monthlyIncome").toString()
            );

            double dailyLimit = Double.parseDouble(
                    request.get("dailyLimit").toString()
            );

            double weeklyLimit = Double.parseDouble(
                    request.get("weeklyLimit").toString()
            );

            double fifteenDayLimit = Double.parseDouble(
                    request.get("fifteenDayLimit").toString()
            );

            double monthlyLimit = Double.parseDouble(
                    request.get("monthlyLimit").toString()
            );

            if (monthlyIncome <= 0
                    || dailyLimit <= 0
                    || weeklyLimit <= 0
                    || fifteenDayLimit <= 0
                    || monthlyLimit <= 0) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Income and all limits must be greater than 0."
                        )
                );
            }

            if (monthlyLimit > monthlyIncome) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Monthly limit cannot be greater than monthly income."
                        )
                );
            }

            if (!updateService.canUpdate(userId)) {
                return ResponseEntity
                        .status(429)
                        .body(
                                Map.of(
                                        "message",
                                        "You have already changed your budget plan 2 times this month.",
                                        "remainingUpdates",
                                        0
                                )
                        );
            }

            Income income = incomeRepository
                    .findByUserId(userId)
                    .orElse(new Income());

            income.setUserId(userId);
            income.setMonthlyIncome(monthlyIncome);
            income.setUpdatedAt(
                    java.time.LocalDateTime.now()
            );

            incomeRepository.save(income);

            dailyLimitService.setDailyLimit(
                    userId,
                    dailyLimit
            );

            weeklyLimitService.setWeeklyLimit(
                    userId,
                    weeklyLimit
            );

            fifteenDayLimitService.setFifteenDayLimit(
                    userId,
                    fifteenDayLimit
            );

            monthlyLimitService.setMonthlyLimit(
                    userId,
                    monthlyLimit
            );

            // Whole plan = ONE update
            updateService.recordUpdate(userId);

            double savings =
                    Math.max(
                            0,
                            monthlyIncome - monthlyLimit
                    );

            Map<String, Object> response =
                    new HashMap<>();

            response.put("message", "Budget plan saved successfully.");
            response.put("userId", userId);
            response.put("monthlyIncome", monthlyIncome);
            response.put("dailyLimit", dailyLimit);
            response.put("weeklyLimit", weeklyLimit);
            response.put(
                    "fifteenDayLimit",
                    fifteenDayLimit
            );
            response.put(
                    "monthlyLimit",
                    monthlyLimit
            );
            response.put("savings", savings);

            response.put(
                    "remainingUpdates",
                    updateService
                            .getRemainingUpdates(userId)
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Invalid budget plan data."
                    )
            );
        }
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<?> getStatus(
            @PathVariable Long userId) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "usedUpdates",
                updateService.getUsedUpdates(userId)
        );

        response.put(
                "remainingUpdates",
                updateService
                        .getRemainingUpdates(userId)
        );

        response.put(
                "maxUpdatesPerMonth",
                2
        );

        response.put(
                "canUpdate",
                updateService.canUpdate(userId)
        );

        return ResponseEntity.ok(response);
    }
}