package com.expense.expense_tracker.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expense.expense_tracker.model.Expense;
import com.expense.expense_tracker.repository.ExpenseRepository;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;

    public ExpenseController(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    // =====================================================
    // ADD EXPENSE
    // =====================================================

    @PostMapping
    public ResponseEntity<?> addExpense(@RequestBody Expense expense) {

        if (expense.getDateTime() == null) {
            expense.setDateTime(LocalDateTime.now());
        }

        if (expense.getSource() == null ||
                expense.getSource().isBlank()) {

            expense.setSource("MANUAL");
        }

        // Prevent duplicate automatic transactions
        if (expense.getTransactionRef() != null &&
                !expense.getTransactionRef().isBlank()) {

            boolean alreadyExists =
                    expenseRepository.existsByTransactionRef(
                            expense.getTransactionRef()
                    );

            if (alreadyExists) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(
                            "Duplicate transaction. Expense already exists."
                        );
            }
        }

        Expense savedExpense =
                expenseRepository.save(expense);

        return ResponseEntity.ok(savedExpense);
    }


    // =====================================================
    // GET ALL EXPENSES
    // =====================================================

    @GetMapping
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }


    // =====================================================
    // UPDATE CATEGORY + REASON
    // =====================================================

    @PutMapping("/{id}/details")
    public ResponseEntity<?> updateExpenseDetails(
            @PathVariable Long id,
            @RequestBody Expense updates) {

        return expenseRepository.findById(id)
                .map(expense -> {

                    // Update category only if received
                    if (updates.getCategory() != null) {
                        expense.setCategory(
                                updates.getCategory()
                        );
                    }

                    // Update reason only if received
                    if (updates.getReason() != null) {
                        expense.setReason(
                                updates.getReason()
                        );
                    }

                    Expense savedExpense =
                            expenseRepository.save(expense);

                    return ResponseEntity.ok(savedExpense);
                })
                .orElseGet(() ->
                        ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(null)
                );
    }
}