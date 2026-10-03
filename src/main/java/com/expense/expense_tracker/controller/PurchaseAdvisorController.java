package com.expense.expense_tracker.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.expense.expense_tracker.model.Expense;
import com.expense.expense_tracker.model.Income;
import com.expense.expense_tracker.repository.ExpenseRepository;
import com.expense.expense_tracker.repository.IncomeRepository;

@RestController
@RequestMapping("/api/purchase-advisor")
public class PurchaseAdvisorController {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final RestClient restClient;

    public PurchaseAdvisorController(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository) {

        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkPurchase(
            @RequestBody Map<String, Object> request) {

        try {

            // --------------------------------
            // 1. Validate request
            // --------------------------------

            if (request.get("userId") == null
                    || request.get("productName") == null
                    || request.get("price") == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "userId, productName and price are required."
                        )
                );
            }

            Long userId = Long.valueOf(
                    request.get("userId").toString()
            );

            String productName =
                    request.get("productName")
                            .toString()
                            .trim();

            double price = Double.parseDouble(
                    request.get("price").toString()
            );

            if (productName.isBlank()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Product name is required."
                        )
                );
            }

            if (price <= 0) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Product price must be greater than 0."
                        )
                );
            }

            // --------------------------------
            // 2. Get monthly income
            // --------------------------------

            Income income =
                    incomeRepository
                            .findByUserId(userId)
                            .orElse(null);

            if (income == null
                    || income.getMonthlyIncome() <= 0) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Please set monthly income first."
                        )
                );
            }

            double monthlyIncome =
                    income.getMonthlyIncome();

            // --------------------------------
            // 3. Get current month expenses
            // --------------------------------

            YearMonth currentMonth =
                    YearMonth.now();

            LocalDateTime start =
                    currentMonth
                            .atDay(1)
                            .atStartOfDay();

            LocalDateTime end =
                    currentMonth
                            .atEndOfMonth()
                            .atTime(23, 59, 59);

            List<Expense> expenses =
                    expenseRepository
                            .findByUserIdAndDateTimeBetween(
                                    userId,
                                    start,
                                    end
                            );

            double spentThisMonth =
                    expenses.stream()
                            .mapToDouble(Expense::getAmount)
                            .sum();

            // --------------------------------
            // 4. Smart Budget calculations
            // --------------------------------

            double savingsTarget =
                    monthlyIncome * 0.20;

            double safeMonthlyBudget =
                    monthlyIncome - savingsTarget;

            double safeBudgetRemaining =
                    Math.max(
                            safeMonthlyBudget
                                    - spentThisMonth,
                            0
                    );

            int daysRemaining =
                    currentMonth.lengthOfMonth()
                            - LocalDate.now()
                            .getDayOfMonth()
                            + 1;

            double safeDailySpend =
                    daysRemaining > 0
                            ? safeBudgetRemaining
                            / daysRemaining
                            : 0;

            // --------------------------------
            // 5. Purchase impact
            // --------------------------------

            double budgetAfterPurchase =
                    safeBudgetRemaining - price;

            double incomeAfterPurchase =
                    monthlyIncome
                            - spentThisMonth
                            - price;

            double priceIncomePercentage =
                    monthlyIncome > 0
                            ? (price / monthlyIncome) * 100
                            : 0;

            double priceRemainingBudgetPercentage =
                    safeBudgetRemaining > 0
                            ? (price / safeBudgetRemaining) * 100
                            : 100;

            // --------------------------------
            // 6. Deterministic decision engine
            // --------------------------------

            String decision;
            int affordabilityScore;
            String reason;

            if (price > safeBudgetRemaining) {

                decision = "AVOID";
                affordabilityScore = 15;

                reason =
                        "This purchase is higher than your "
                        + "remaining safe monthly budget and "
                        + "would put your savings plan at risk.";

            } else if (priceRemainingBudgetPercentage > 60) {

                decision = "WAIT";
                affordabilityScore = 40;

                reason =
                        "You can technically afford this purchase, "
                        + "but it would use a large part of your "
                        + "remaining safe budget.";

            } else if (priceRemainingBudgetPercentage > 35) {

                decision = "WAIT";
                affordabilityScore = 60;

                reason =
                        "The purchase is possible, but waiting "
                        + "would give you more financial flexibility "
                        + "for the rest of the month.";

            } else {

                decision = "BUY";
                affordabilityScore = 85;

                reason =
                        "This purchase fits within your current "
                        + "safe budget without immediately putting "
                        + "your savings target at risk.";
            }

            // Expensive relative to monthly income
            if (priceIncomePercentage >= 50
                    && !decision.equals("AVOID")) {

                decision = "WAIT";

                affordabilityScore =
                        Math.min(
                                affordabilityScore,
                                45
                        );

                reason =
                        "This product costs a large percentage "
                        + "of your monthly income. Waiting or "
                        + "saving separately for it would be safer.";
            }

            // --------------------------------
            // 7. Gemini AI explanation
            // --------------------------------

            String aiAdvice = null;

            String apiKey =
                    System.getenv("GEMINI_API_KEY");

            if (apiKey != null
                    && !apiKey.isBlank()) {

                String prompt = """
                        You are SpendIQ AI Purchase Advisor.

                        Your job is to explain an already calculated
                        financial purchase decision.

                        IMPORTANT RULES:

                        - Do NOT change the BUY, WAIT or AVOID decision.
                        - Do NOT change the affordability score.
                        - Use ONLY the financial data provided below.
                        - Never invent income, expenses, prices or balances.
                        - Keep the answer under 100 words.
                        - Use simple and friendly English.
                        - Use ₹ for Indian Rupees.
                        - Give 2 practical suggestions.
                        - Do not recommend loans, credit cards,
                          investments or borrowing money.
                        - Do not claim to know product quality,
                          market price or discounts.
                        - Focus only on whether this purchase fits
                          the user's current budget.

                        PRODUCT:

                        Product Name: %s
                        Product Price: ₹%.2f

                        USER FINANCIAL DATA:

                        Monthly Income: ₹%.2f
                        Spent This Month: ₹%.2f
                        Savings Target: ₹%.2f
                        Safe Budget Remaining: ₹%.2f
                        Safe Daily Spend: ₹%.2f
                        Days Remaining: %d
                        Budget After Purchase: ₹%.2f

                        CALCULATED DECISION:

                        Decision: %s
                        Affordability Score: %d/100
                        Reason: %s

                        Explain clearly why this decision makes sense
                        for the user's current budget.
                        """.formatted(
                        productName,
                        price,
                        monthlyIncome,
                        spentThisMonth,
                        savingsTarget,
                        safeBudgetRemaining,
                        safeDailySpend,
                        daysRemaining,
                        budgetAfterPurchase,
                        decision,
                        affordabilityScore,
                        reason
                );

                Map<String, Object> requestBody =
                        Map.of(
                                "contents",
                                List.of(
                                        Map.of(
                                                "parts",
                                                List.of(
                                                        Map.of(
                                                                "text",
                                                                prompt
                                                        )
                                                )
                                        )
                                )
                        );

                try {

                    Map<?, ?> geminiResponse =
                            restClient
                                    .post()
                                    .uri(
                                            "/v1beta/models/"
                                            + "gemini-3.5-flash-lite"
                                            + ":generateContent"
                                    )
                                    .header(
                                            "x-goog-api-key",
                                            apiKey
                                    )
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .body(requestBody)
                                    .retrieve()
                                    .body(Map.class);

                    aiAdvice =
                            extractAdvice(
                                    geminiResponse
                            );

                } catch (Exception e) {

                    System.out.println(
                            "Purchase Advisor Gemini Error: "
                                    + e.getMessage()
                    );
                }
            }

            // --------------------------------
            // 8. Fallback if Gemini unavailable
            // --------------------------------

            if (aiAdvice == null
                    || aiAdvice.isBlank()) {

                aiAdvice =
                        reason
                        + " Your affordability score is "
                        + affordabilityScore
                        + "/100. Consider your remaining "
                        + "monthly budget before completing "
                        + "the purchase.";
            }

            // --------------------------------
            // 9. Final response
            // --------------------------------

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "userId",
                    userId
            );

            response.put(
                    "productName",
                    productName
            );

            response.put(
                    "productPrice",
                    round(price)
            );

            response.put(
                    "decision",
                    decision
            );

            response.put(
                    "affordabilityScore",
                    affordabilityScore
            );

            response.put(
                    "reason",
                    reason
            );

            response.put(
                    "aiAdvice",
                    aiAdvice
            );

            response.put(
                    "monthlyIncome",
                    round(monthlyIncome)
            );

            response.put(
                    "spentThisMonth",
                    round(spentThisMonth)
            );

            response.put(
                    "savingsTarget",
                    round(savingsTarget)
            );

            response.put(
                    "safeMonthlyBudget",
                    round(safeMonthlyBudget)
            );

            response.put(
                    "safeBudgetRemaining",
                    round(safeBudgetRemaining)
            );

            response.put(
                    "safeDailySpend",
                    round(safeDailySpend)
            );

            response.put(
                    "daysRemaining",
                    daysRemaining
            );

            response.put(
                    "budgetAfterPurchase",
                    round(budgetAfterPurchase)
            );

            response.put(
                    "incomeAfterPurchase",
                    round(incomeAfterPurchase)
            );

            response.put(
                    "priceAsPercentOfIncome",
                    round(priceIncomePercentage)
            );

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Invalid userId or product price."
                    )
            );

        } catch (Exception e) {

            System.out.println(
                    "Purchase Advisor Error: "
                            + e.getMessage()
            );

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "message",
                                    "Unable to analyse purchase."
                            )
                    );
        }
    }

    private String extractAdvice(
            Map<?, ?> response) {

        if (response == null) {
            return null;
        }

        try {

            List<?> candidates =
                    (List<?>) response.get(
                            "candidates"
                    );

            if (candidates == null
                    || candidates.isEmpty()) {

                return null;
            }

            Map<?, ?> candidate =
                    (Map<?, ?>) candidates.get(0);

            Map<?, ?> content =
                    (Map<?, ?>) candidate.get(
                            "content"
                    );

            List<?> parts =
                    (List<?>) content.get(
                            "parts"
                    );

            if (parts == null
                    || parts.isEmpty()) {

                return null;
            }

            Map<?, ?> firstPart =
                    (Map<?, ?>) parts.get(0);

            Object text =
                    firstPart.get("text");

            return text == null
                    ? null
                    : text.toString();

        } catch (Exception e) {

            return null;
        }
    }

    private double round(double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}