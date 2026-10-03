package com.expense.expense_tracker.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expense.expense_tracker.service.SpendIQEmailService;

@RestController
@RequestMapping("/api/report-email")
public class SpendIQEmailController {

    private final SpendIQEmailService emailService;

    public SpendIQEmailController(
            SpendIQEmailService emailService) {

        this.emailService = emailService;
    }

    // =========================================================
    // SEND CURRENT PERIOD REPORT
    //
    // Example:
    // GET /api/report-email/1/current
    // =========================================================

    @GetMapping("/{userId}/current")
    public ResponseEntity<Map<String, Object>> sendCurrentReport(
            @PathVariable Long userId) {

        emailService.sendCurrentPeriodReport(userId);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "message",
                "SpendIQ report email sent successfully.");

        response.put("userId", userId);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // SEND CUSTOM DATE RANGE REPORT
    //
    // Example:
    // GET /api/report-email/1?start=2026-09-11&end=2026-09-20
    // =========================================================

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> sendCustomReport(
            @PathVariable Long userId,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE)
            LocalDate start,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE)
            LocalDate end) {

        emailService.sendReport(
                userId,
                start,
                end);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "message",
                "SpendIQ report email sent successfully.");

        response.put("userId", userId);
        response.put("startDate", start);
        response.put("endDate", end);

        return ResponseEntity.ok(response);
    }
}