package com.expense.expense_tracker.controller;

import java.time.LocalDateTime;
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

@RestController
@RequestMapping("/api/income")
public class IncomeController {

    private final IncomeRepository incomeRepository;

    public IncomeController(
            IncomeRepository incomeRepository) {

        this.incomeRepository = incomeRepository;
    }

    // ==========================================
    // SAVE OR UPDATE MONTHLY INCOME
    // ==========================================

    @PostMapping
    public ResponseEntity<?> saveIncome(
            @RequestBody Income request) {

        if (request.getUserId() == null) {
            return ResponseEntity
                    .badRequest()
                    .body("User ID is required.");
        }

        if (request.getMonthlyIncome() <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body("Monthly income must be greater than 0.");
        }

        Income income = incomeRepository
                .findByUserId(request.getUserId())
                .orElse(new Income());

        income.setUserId(request.getUserId());

        income.setMonthlyIncome(
                request.getMonthlyIncome()
        );

        income.setUpdatedAt(
                LocalDateTime.now()
        );

        Income saved =
                incomeRepository.save(income);

        return ResponseEntity.ok(saved);
    }


    // ==========================================
    // GET USER MONTHLY INCOME
    // ==========================================

    @GetMapping("/{userId}")
    public ResponseEntity<?> getIncome(
            @PathVariable Long userId) {

        return incomeRepository
                .findByUserId(userId)
                .<ResponseEntity<?>>map(
                        ResponseEntity::ok
                )
                .orElseGet(
                        () -> ResponseEntity
                                .ok(
                                    Map.of(
                                        "userId", userId,
                                        "monthlyIncome", 0
                                    )
                                )
                );
    }
}