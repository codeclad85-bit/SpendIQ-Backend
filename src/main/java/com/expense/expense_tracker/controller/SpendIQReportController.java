package com.expense.expense_tracker.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expense.expense_tracker.service.SpendIQReportService;

@RestController
@RequestMapping("/api/reports")
public class SpendIQReportController {

    private final SpendIQReportService reportService;

    public SpendIQReportController(
            SpendIQReportService reportService) {

        this.reportService = reportService;
    }

    // =========================================================
    // DOWNLOAD CURRENT 10-DAY PERIOD REPORT
    //
    // Periods:
    // 1 - 10
    // 11 - 20
    // 21 - End of Month
    //
    // Example:
    // GET /api/reports/1/current
    // =========================================================

    @GetMapping(
            value = "/{userId}/current",
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadCurrentReport(
            @PathVariable Long userId) {

        byte[] pdf =
                reportService
                        .generateCurrentPeriodReport(userId);

        String fileName =
                "SpendIQ_Report_User_"
                        + userId
                        + "_"
                        + LocalDate.now()
                        + ".pdf";

        return buildPdfResponse(
                pdf,
                fileName);
    }

    // =========================================================
    // DOWNLOAD REPORT FOR CUSTOM DATE RANGE
    //
    // Example:
    // GET /api/reports/1?start=2026-09-11&end=2026-09-20
    // =========================================================

    @GetMapping(
            value = "/{userId}",
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable Long userId,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE)
            LocalDate start,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE)
            LocalDate end) {

        byte[] pdf =
                reportService.generateReport(
                        userId,
                        start,
                        end);

        String fileName =
                "SpendIQ_Report_"
                        + start
                        + "_to_"
                        + end
                        + ".pdf";

        return buildPdfResponse(
                pdf,
                fileName);
    }

    // =========================================================
    // PDF RESPONSE
    // =========================================================

    private ResponseEntity<byte[]> buildPdfResponse(
            byte[] pdf,
            String fileName) {

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_PDF);

        headers.setContentDisposition(
                ContentDisposition
                        .attachment()
                        .filename(fileName)
                        .build());

        headers.setContentLength(
                pdf.length);

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(pdf);
    }
}