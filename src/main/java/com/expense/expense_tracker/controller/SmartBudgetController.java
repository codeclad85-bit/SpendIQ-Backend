package com.expense.expense_tracker.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expense.expense_tracker.model.Expense;
import com.expense.expense_tracker.model.Income;
import com.expense.expense_tracker.model.MonthlyLimit;
import com.expense.expense_tracker.repository.ExpenseRepository;
import com.expense.expense_tracker.repository.IncomeRepository;
import com.expense.expense_tracker.service.MonthlyLimitService;

@RestController
@RequestMapping("/api/smart-budget")
public class SmartBudgetController {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final MonthlyLimitService monthlyLimitService;

    public SmartBudgetController(
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            MonthlyLimitService monthlyLimitService) {

        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.monthlyLimitService = monthlyLimitService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getSmartBudget(
            @PathVariable Long userId) {

        // ==========================================
        // 1. GET MONTHLY LIMIT
        // Smart Budget is based on Monthly Limit
        // ==========================================

        MonthlyLimit monthlyLimit =
                monthlyLimitService.getMonthlyLimit(userId);

        if (monthlyLimit == null ||
                monthlyLimit.getLimitAmount() <= 0) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Please set monthly limit first."
                    )
            );
        }

        double totalBudget =
                monthlyLimit.getLimitAmount();


        // ==========================================
        // 2. GET MONTHLY INCOME
        // Income is only extra financial context.
        // Smart Budget base remains Monthly Limit.
        // ==========================================

        Income income =
                incomeRepository
                        .findByUserId(userId)
                        .orElse(null);

        double monthlyIncome = 0;

        if (income != null) {
            monthlyIncome =
                    income.getMonthlyIncome();
        }


        // ==========================================
        // 3. CURRENT MONTH DATE RANGE
        // ==========================================

        YearMonth currentMonth =
                YearMonth.now();

        LocalDateTime startOfMonth =
                currentMonth
                        .atDay(1)
                        .atStartOfDay();

        LocalDateTime startOfNextMonth =
                currentMonth
                        .plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();


        // ==========================================
        // 4. GET CURRENT MONTH EXPENSES
        // ==========================================

        List<Expense> expenses =
                expenseRepository
                        .findByUserIdAndDateTimeBetween(
                                userId,
                                startOfMonth,
                                startOfNextMonth
                        );


        // ==========================================
        // 5. TOTAL SPENDING
        // ==========================================

        double spentThisMonth =
                expenses.stream()
                        .mapToDouble(Expense::getAmount)
                        .sum();


        // ==========================================
        // 6. CATEGORY DEFAULT WEIGHTS
        //
        // These provide a sensible starting plan.
        // Actual spending pattern is also considered.
        // ==========================================

        Map<String, Double> defaultWeights =
                new LinkedHashMap<>();

        defaultWeights.put("Food", 0.30);
        defaultWeights.put("Bills", 0.20);
        defaultWeights.put("Travel", 0.15);
        defaultWeights.put("Shopping", 0.15);
        defaultWeights.put("Entertainment", 0.10);
        defaultWeights.put("Other", 0.10);


        // ==========================================
        // 7. CURRENT CATEGORY SPENDING
        // ==========================================

        Map<String, Double> categorySpending =
                new LinkedHashMap<>();

        for (String category :
                defaultWeights.keySet()) {

            categorySpending.put(
                    category,
                    0.0
            );
        }

        for (Expense expense : expenses) {

            String category =
                    normalizeCategory(
                            expense.getCategory()
                    );

            double oldAmount =
                    categorySpending.getOrDefault(
                            category,
                            0.0
                    );

            categorySpending.put(
                    category,
                    oldAmount + expense.getAmount()
            );
        }


        // ==========================================
        // 8. CALCULATE SMART CATEGORY WEIGHTS
        //
        // 60% = stable recommended pattern
        // 40% = user's actual spending pattern
        //
        // This keeps Smart Budget personalized
        // without making it unstable.
        // ==========================================

        Map<String, Double> smartWeights =
                new LinkedHashMap<>();

        double totalCategorizedSpending =
                categorySpending.values()
                        .stream()
                        .mapToDouble(Double::doubleValue)
                        .sum();

        for (String category :
                defaultWeights.keySet()) {

            double defaultWeight =
                    defaultWeights.get(category);

            double historyWeight =
                    defaultWeight;

            if (totalCategorizedSpending > 0) {

                historyWeight =
                        categorySpending.get(category)
                                / totalCategorizedSpending;
            }

            double smartWeight =
                    (defaultWeight * 0.60)
                            +
                    (historyWeight * 0.40);

            smartWeights.put(
                    category,
                    smartWeight
            );
        }


        // ==========================================
        // 9. NORMALIZE WEIGHTS
        // Ensures all category budgets together
        // equal exactly 100% of Monthly Limit.
        // ==========================================

        double weightTotal =
                smartWeights.values()
                        .stream()
                        .mapToDouble(Double::doubleValue)
                        .sum();

        if (weightTotal <= 0) {
            weightTotal = 1;
        }


        // ==========================================
        // 10. CREATE CATEGORY BUDGET PLAN
        // ==========================================

        List<Map<String, Object>> categories =
                new ArrayList<>();

        double allocatedSoFar = 0;

        int index = 0;

        int categoryCount =
                smartWeights.size();

        for (Map.Entry<String, Double> entry :
                smartWeights.entrySet()) {

            index++;

            String category =
                    entry.getKey();

            double categoryBudget;

            // Last category receives remaining amount.
            // This prevents rounding difference.
            if (index == categoryCount) {

                categoryBudget =
                        totalBudget
                                - allocatedSoFar;

            } else {

                categoryBudget =
                        totalBudget
                                *
                        (entry.getValue()
                                / weightTotal);

                categoryBudget =
                        round(categoryBudget);

                allocatedSoFar +=
                        categoryBudget;
            }

            categoryBudget =
                    Math.max(
                            categoryBudget,
                            0
                    );

            double spent =
                    categorySpending
                            .getOrDefault(
                                    category,
                                    0.0
                            );

            double remaining =
                    categoryBudget - spent;

            double percentage = 0;

            if (categoryBudget > 0) {

                percentage =
                        (spent /
                                categoryBudget)
                                * 100;
            }

            String categoryStatus;

            if (spent > categoryBudget) {

                categoryStatus =
                        "OVER_BUDGET";

            } else if (percentage >= 80) {

                categoryStatus =
                        "WARNING";

            } else {

                categoryStatus =
                        "ON_TRACK";
            }

            Map<String, Object>
                    categoryData =
                    new LinkedHashMap<>();

            categoryData.put(
                    "category",
                    category
            );

            categoryData.put(
                    "recommendedBudget",
                    round(categoryBudget)
            );

            categoryData.put(
                    "spent",
                    round(spent)
            );

            categoryData.put(
                    "remaining",
                    round(
                            Math.max(
                                    remaining,
                                    0
                            )
                    )
            );

            categoryData.put(
                    "percentage",
                    round(percentage)
            );

            categoryData.put(
                    "status",
                    categoryStatus
            );

            categories.add(
                    categoryData
            );
        }


        // ==========================================
        // 11. OVERALL MONTHLY BUDGET DATA
        // ==========================================

        double budgetRemaining =
                totalBudget - spentThisMonth;

        double spendingPercentage = 0;

        if (totalBudget > 0) {

            spendingPercentage =
                    (spentThisMonth /
                            totalBudget)
                            * 100;
        }


        // ==========================================
        // 12. DAYS REMAINING
        // ==========================================

        LocalDate today =
                LocalDate.now();

        int daysRemaining =
                currentMonth.lengthOfMonth()
                        - today.getDayOfMonth()
                        + 1;


        // ==========================================
        // 13. SAFE DAILY SPENDING
        // ==========================================

        double safeDailySpend = 0;

        if (budgetRemaining > 0 &&
                daysRemaining > 0) {

            safeDailySpend =
                    budgetRemaining /
                            daysRemaining;
        }


        // ==========================================
        // 14. OVERALL STATUS
        // ==========================================

        String status;

        if (spentThisMonth > totalBudget) {

            status =
                    "LIMIT_EXCEEDED";

        } else if (spendingPercentage >= 90) {

            status =
                    "CRITICAL";

        } else if (spendingPercentage >= 80) {

            status =
                    "CAUTION";

        } else {

            status =
                    "ON_TRACK";
        }


        // ==========================================
        // 15. SAVINGS CONTEXT
        // ==========================================

        double savingsTarget = 0;

        if (monthlyIncome > totalBudget) {

            savingsTarget =
                    monthlyIncome
                            - totalBudget;
        }


        // ==========================================
        // 16. FINAL RESPONSE
        // ==========================================

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "userId",
                userId
        );

        response.put(
                "monthlyIncome",
                round(monthlyIncome)
        );

        response.put(
                "monthlyLimit",
                round(totalBudget)
        );

        // Keep this key for existing frontend
        response.put(
                "safeMonthlyBudget",
                round(totalBudget)
        );

        response.put(
                "spentThisMonth",
                round(spentThisMonth)
        );

        response.put(
                "remainingBudget",
                round(
                        Math.max(
                                budgetRemaining,
                                0
                        )
                )
        );

        // Keep old frontend compatibility
        response.put(
                "safeBudgetRemaining",
                round(
                        Math.max(
                                budgetRemaining,
                                0
                        )
                )
        );

        response.put(
                "savingsTarget",
                round(savingsTarget)
        );

        response.put(
                "remainingIncome",
                round(
                        monthlyIncome
                                - spentThisMonth
                )
        );

        response.put(
                "daysRemaining",
                daysRemaining
        );

        response.put(
                "safeDailySpend",
                round(safeDailySpend)
        );

        response.put(
                "spendingPercentage",
                round(spendingPercentage)
        );

        response.put(
                "status",
                status
        );

        response.put(
                "categories",
                categories
        );

        return ResponseEntity.ok(response);
    }


    // ==========================================
    // NORMALIZE EXPENSE CATEGORY
    // ==========================================

    private String normalizeCategory(
            String category) {

        if (category == null ||
                category.isBlank()) {

            return "Other";
        }

        String value =
                category.trim()
                        .toLowerCase();

        if (value.contains("food") ||
                value.contains("grocery") ||
                value.contains("restaurant")) {

            return "Food";
        }

        if (value.contains("bill") ||
                value.contains("electric") ||
                value.contains("recharge") ||
                value.contains("rent") ||
                value.contains("utility")) {

            return "Bills";
        }

        if (value.contains("travel") ||
                value.contains("transport") ||
                value.contains("fuel") ||
                value.contains("petrol") ||
                value.contains("cab") ||
                value.contains("auto")) {

            return "Travel";
        }

        if (value.contains("shopping") ||
                value.contains("clothes") ||
                value.contains("fashion")) {

            return "Shopping";
        }

        if (value.contains("entertainment") ||
                value.contains("movie") ||
                value.contains("game") ||
                value.contains("subscription")) {

            return "Entertainment";
        }

        return "Other";
    }


    // ==========================================
    // ROUND MONEY VALUES
    // ==========================================

    private double round(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}