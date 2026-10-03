package com.expense.expense_tracker.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.expense.expense_tracker.model.Expense;
import com.expense.expense_tracker.model.Income;
import com.expense.expense_tracker.model.MonthlyLimit;
import com.expense.expense_tracker.repository.ExpenseRepository;
import com.expense.expense_tracker.repository.IncomeRepository;
import com.expense.expense_tracker.service.MonthlyLimitService;

@RestController
@RequestMapping("/api/ai-advisor")
public class AiAdvisorController {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final MonthlyLimitService monthlyLimitService;
    private final RestClient restClient;

    public AiAdvisorController(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            MonthlyLimitService monthlyLimitService) {

        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.monthlyLimitService = monthlyLimitService;

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    @GetMapping("/budget/{userId}")
    public ResponseEntity<?> getBudgetAdvice(@PathVariable Long userId) {

        // SpendIQ uses the user's MONTHLY LIMIT as the main budget.
        // MonthlyLimitService returns the MonthlyLimit entity, not a double.
        MonthlyLimit monthlyLimitRecord = monthlyLimitService.getMonthlyLimit(userId);

        if (monthlyLimitRecord == null || monthlyLimitRecord.getLimitAmount() <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Please set monthly limit first."
            ));
        }

        double monthlyLimit = monthlyLimitRecord.getLimitAmount();

        // Income is useful context only. AI Advisor must still work without it.
        Income income = incomeRepository.findByUserId(userId).orElse(null);
        double monthlyIncome =
                income != null && income.getMonthlyIncome() > 0
                        ? income.getMonthlyIncome()
                        : 0.0;

        YearMonth month = YearMonth.now();
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        List<Expense> expenses =
                expenseRepository.findByUserIdAndDateTimeBetween(
                        userId,
                        start,
                        end
                );

        double spentThisMonth = expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        double remainingBudget =
                Math.max(monthlyLimit - spentThisMonth, 0);

        int daysRemaining =
                month.lengthOfMonth()
                        - LocalDate.now().getDayOfMonth()
                        + 1;

        double safeDailySpend =
                daysRemaining > 0
                        ? remainingBudget / daysRemaining
                        : 0;

        double spendingPercentage =
                monthlyLimit > 0
                        ? (spentThisMonth / monthlyLimit) * 100
                        : 0;

        String status;

        if (spentThisMonth > monthlyLimit) {
            status = "LIMIT_EXCEEDED";
        } else if (spendingPercentage >= 90) {
            status = "CRITICAL";
        } else if (spendingPercentage >= 80) {
            status = "CAUTION";
        } else {
            status = "ON_TRACK";
        }

        // Always prepare a safe local advisor message.
        // This means the endpoint NEVER becomes useless just because Gemini
        // is temporarily unavailable or the environment variable is missing.
        String advice = buildFallbackAdvice(
                monthlyLimit,
                spentThisMonth,
                remainingBudget,
                safeDailySpend,
                spendingPercentage,
                status
        );

        String apiKey = System.getenv("GEMINI_API_KEY");

        // Gemini enhances the verified SpendIQ analysis when available.
        if (apiKey != null && !apiKey.isBlank()) {

            String prompt = """
                    You are SpendIQ AI Budget Advisor.

                    Give practical, short and friendly personal budgeting advice.

                    IMPORTANT RULES:
                    - Use ONLY the verified financial numbers below.
                    - The user's Monthly Limit is the main spending budget.
                    - Never invent income, expenses, balances or transactions.
                    - Never change the calculated safe daily spend.
                    - Keep the response under 120 words.
                    - Use simple English.
                    - Mention Indian Rupees using ₹.
                    - Explain whether spending is on track, getting high, or over budget.
                    - Give 2 or 3 useful actions for the rest of the month.
                    - Do not recommend loans, credit cards, investments or risky products.

                    VERIFIED USER DATA:
                    Monthly Income (context only): ₹%.2f
                    Monthly Limit: ₹%.2f
                    Spent This Month: ₹%.2f
                    Remaining Monthly Budget: ₹%.2f
                    Budget Used: %.2f%%
                    Days Remaining: %d
                    Safe Daily Spend: ₹%.2f
                    Budget Status: %s
                    """.formatted(
                    monthlyIncome,
                    monthlyLimit,
                    spentThisMonth,
                    remainingBudget,
                    spendingPercentage,
                    daysRemaining,
                    safeDailySpend,
                    status
            );

            Map<String, Object> requestBody = Map.of(
                    "contents",
                    List.of(
                            Map.of(
                                    "parts",
                                    List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    )
            );

            try {
                Map<?, ?> response = restClient
                        .post()
                        .uri("/v1beta/models/gemini-3.5-flash-lite:generateContent")
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);

                String geminiAdvice = extractAdvice(response);

                if (geminiAdvice != null && !geminiAdvice.isBlank()) {
                    advice = geminiAdvice.trim();
                }

            } catch (Exception e) {
                // Do not fail the whole Advisor.
                // Verified SpendIQ analysis remains available.
                System.out.println(
                        "Gemini Advisor unavailable; using SpendIQ fallback: "
                                + e.getMessage()
                );
            }
        }

        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("userId", userId),
                Map.entry("monthlyIncome", round(monthlyIncome)),
                Map.entry("monthlyLimit", round(monthlyLimit)),
                Map.entry("safeMonthlyBudget", round(monthlyLimit)),
                Map.entry("spentThisMonth", round(spentThisMonth)),
                Map.entry("remainingBudget", round(remainingBudget)),
                Map.entry("safeBudgetRemaining", round(remainingBudget)),
                Map.entry("safeDailySpend", round(safeDailySpend)),
                Map.entry("daysRemaining", daysRemaining),
                Map.entry("spendingPercentage", round(spendingPercentage)),
                Map.entry("status", status),
                Map.entry("advice", advice)
        ));
    }

    private String buildFallbackAdvice(
            double monthlyLimit,
            double spent,
            double remaining,
            double safeDailySpend,
            double percentage,
            String status) {

        if ("LIMIT_EXCEEDED".equals(status)) {
            return String.format(
                    "You have crossed your monthly budget of ₹%.0f. "
                            + "Your spending is ₹%.0f. For the rest of the month, "
                            + "focus only on essential expenses and avoid unnecessary purchases.",
                    monthlyLimit,
                    spent
            );
        }

        if ("CRITICAL".equals(status)) {
            return String.format(
                    "You have already used %.1f%% of your monthly budget. "
                            + "Only ₹%.0f is left. Keep upcoming spending minimal "
                            + "and prioritise essential expenses.",
                    percentage,
                    remaining
            );
        }

        if ("CAUTION".equals(status)) {
            return String.format(
                    "Your spending is getting close to the monthly limit. "
                            + "You have ₹%.0f remaining. Try to keep daily spending "
                            + "around ₹%.0f or less for the rest of the month.",
                    remaining,
                    safeDailySpend
            );
        }

        return String.format(
                "Your spending is on track. You have used %.1f%% of your monthly budget "
                        + "and ₹%.0f is still available. You can safely target around "
                        + "₹%.0f per day while continuing to avoid unnecessary spending.",
                percentage,
                remaining,
                safeDailySpend
        );
    }

    private String extractAdvice(Map<?, ?> response) {

        if (response == null) {
            return null;
        }

        try {
            List<?> candidates = (List<?>) response.get("candidates");

            if (candidates == null || candidates.isEmpty()) {
                return null;
            }

            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) candidate.get("content");

            if (content == null) {
                return null;
            }

            List<?> parts = (List<?>) content.get("parts");

            if (parts == null || parts.isEmpty()) {
                return null;
            }

            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
            Object text = firstPart.get("text");

            return text == null ? null : text.toString();

        } catch (Exception e) {
            return null;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
