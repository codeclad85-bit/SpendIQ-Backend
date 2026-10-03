package com.expense.expense_tracker.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "budget_plan_update",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "userId")
    }
)
public class BudgetPlanUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private int updateCount;

    private int updateMonth;

    private int updateYear;

    private LocalDateTime lastUpdatedAt;

    public BudgetPlanUpdate() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    public int getUpdateMonth() {
        return updateMonth;
    }

    public void setUpdateMonth(int updateMonth) {
        this.updateMonth = updateMonth;
    }

    public int getUpdateYear() {
        return updateYear;
    }

    public void setUpdateYear(int updateYear) {
        this.updateYear = updateYear;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}