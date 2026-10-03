package com.expense.expense_tracker.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.expense.expense_tracker.model.DailyLimit;
import com.expense.expense_tracker.model.Expense;
import com.expense.expense_tracker.model.FifteenDayLimit;
import com.expense.expense_tracker.model.Income;
import com.expense.expense_tracker.model.MonthlyLimit;
import com.expense.expense_tracker.model.User;
import com.expense.expense_tracker.model.WeeklyLimit;
import com.expense.expense_tracker.repository.ExpenseRepository;
import com.expense.expense_tracker.repository.IncomeRepository;
import com.expense.expense_tracker.repository.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class SpendIQReportService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final IncomeRepository incomeRepository;

    private final DailyLimitService dailyLimitService;
    private final WeeklyLimitService weeklyLimitService;
    private final FifteenDayLimitService fifteenDayLimitService;
    private final MonthlyLimitService monthlyLimitService;

    public SpendIQReportService(
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            IncomeRepository incomeRepository,
            DailyLimitService dailyLimitService,
            WeeklyLimitService weeklyLimitService,
            FifteenDayLimitService fifteenDayLimitService,
            MonthlyLimitService monthlyLimitService) {

        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.incomeRepository = incomeRepository;

        this.dailyLimitService = dailyLimitService;
        this.weeklyLimitService = weeklyLimitService;
        this.fifteenDayLimitService = fifteenDayLimitService;
        this.monthlyLimitService = monthlyLimitService;
    }

    // =========================================================
    // CURRENT REPORT PERIOD
    // 1-10
    // 11-20
    // 21-End of Month
    // =========================================================

    public byte[] generateCurrentPeriodReport(Long userId) {

        LocalDate today = LocalDate.now();

        LocalDate startDate;
        LocalDate endDate;

        int day = today.getDayOfMonth();

        if (day <= 10) {

            startDate = today.withDayOfMonth(1);
            endDate = today.withDayOfMonth(
                    Math.min(10, today.lengthOfMonth()));

        } else if (day <= 20) {

            startDate = today.withDayOfMonth(11);
            endDate = today.withDayOfMonth(
                    Math.min(20, today.lengthOfMonth()));

        } else {

            startDate = today.withDayOfMonth(21);
            endDate = today.withDayOfMonth(
                    today.lengthOfMonth());
        }

        return generateReport(
                userId,
                startDate,
                endDate);
    }

    // =========================================================
    // GENERATE REPORT
    // =========================================================

    public byte[] generateReport(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID is required.");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date.");
        }

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found: " + userId));

        LocalDateTime startDateTime =
                startDate.atStartOfDay();

        LocalDateTime endDateTime =
                endDate.atTime(LocalTime.MAX);

        List<Expense> expenses =
                new ArrayList<>(
                        expenseRepository
                                .findByUserIdAndDateTimeBetween(
                                        userId,
                                        startDateTime,
                                        endDateTime));

        expenses.sort(
                Comparator.comparing(
                                Expense::getDateTime,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()))
                        .reversed());

        double totalSpending =
                expenses.stream()
                        .mapToDouble(Expense::getAmount)
                        .sum();

        Map<String, Double> categoryTotals =
                calculateCategoryTotals(expenses);

        double monthlyIncome =
                loadMonthlyIncome(userId);

        double dailyLimit =
                loadDailyLimit(userId);

        double weeklyLimit =
                loadWeeklyLimit(userId);

        double fifteenDayLimit =
                loadFifteenDayLimit(userId);

        double monthlyLimit =
                loadMonthlyLimit(userId);

        double plannedSavings =
                Math.max(
                        monthlyIncome - monthlyLimit,
                        0);

        return createPdf(
                user,
                startDate,
                endDate,
                expenses,
                categoryTotals,
                totalSpending,
                monthlyIncome,
                dailyLimit,
                weeklyLimit,
                fifteenDayLimit,
                monthlyLimit,
                plannedSavings);
    }

    // =========================================================
    // LOAD INCOME
    // =========================================================

    private double loadMonthlyIncome(Long userId) {

        try {

            Income income =
                    incomeRepository
                            .findByUserId(userId)
                            .orElse(null);

            if (income == null) {
                return 0.0;
            }

            return income.getMonthlyIncome();

        } catch (Exception e) {

            return 0.0;
        }
    }

    // =========================================================
    // LOAD DAILY LIMIT
    // =========================================================

    private double loadDailyLimit(Long userId) {

        try {

            DailyLimit limit =
                    dailyLimitService
                            .getDailyLimit(userId);

            if (limit == null) {
                return 0.0;
            }

            return limit.getLimitAmount();

        } catch (Exception e) {

            return 0.0;
        }
    }

    // =========================================================
    // LOAD WEEKLY LIMIT
    // =========================================================

    private double loadWeeklyLimit(Long userId) {

        try {

            WeeklyLimit limit =
                    weeklyLimitService
                            .getWeeklyLimit(userId);

            if (limit == null) {
                return 0.0;
            }

            return limit.getLimitAmount();

        } catch (Exception e) {

            return 0.0;
        }
    }

    // =========================================================
    // LOAD 15-DAY LIMIT
    // =========================================================

    private double loadFifteenDayLimit(Long userId) {

        try {

            FifteenDayLimit limit =
                    fifteenDayLimitService
                            .getFifteenDayLimit(userId);

            if (limit == null) {
                return 0.0;
            }

            return limit.getLimitAmount();

        } catch (Exception e) {

            return 0.0;
        }
    }

    // =========================================================
    // LOAD MONTHLY LIMIT
    // =========================================================

    private double loadMonthlyLimit(Long userId) {

        try {

            MonthlyLimit limit =
                    monthlyLimitService
                            .getMonthlyLimit(userId);

            if (limit == null) {
                return 0.0;
            }

            return limit.getLimitAmount();

        } catch (Exception e) {

            return 0.0;
        }
    }

    // =========================================================
    // CATEGORY TOTALS
    // =========================================================

    private Map<String, Double> calculateCategoryTotals(
            List<Expense> expenses) {

        Map<String, Double> totals =
                new LinkedHashMap<>();

        for (Expense expense : expenses) {

            String category =
                    safe(expense.getCategory());

            if (category.isBlank()) {
                category = "Other";
            }

            double current =
                    totals.getOrDefault(
                            category,
                            0.0);

            totals.put(
                    category,
                    current + expense.getAmount());
        }

        return totals;
    }

    // =========================================================
    // CREATE PDF
    // =========================================================

    private byte[] createPdf(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            List<Expense> expenses,
            Map<String, Double> categoryTotals,
            double totalSpending,
            double monthlyIncome,
            double dailyLimit,
            double weeklyLimit,
            double fifteenDayLimit,
            double monthlyLimit,
            double plannedSavings) {

        try {

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            Document document =
                    new Document(
                            PageSize.A4,
                            35,
                            35,
                            40,
                            40);

            PdfWriter.getInstance(
                    document,
                    output);

            document.open();

            // ============================
            // FONTS
            // ============================

            Font titleFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            22);

            Font subtitleFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            10);

            Font headingFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            14);

            Font normalFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            9);

            Font boldFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            9);

            // ============================
            // TITLE
            // ============================

            Paragraph title =
                    new Paragraph(
                            "SpendIQ Financial Report",
                            titleFont);

            title.setAlignment(
                    Element.ALIGN_CENTER);

            document.add(title);

            Paragraph subtitle =
                    new Paragraph(
                            "Smart Expense Tracking & Budget Management",
                            subtitleFont);

            subtitle.setAlignment(
                    Element.ALIGN_CENTER);

            document.add(subtitle);

            document.add(
                    new Paragraph(" "));

            // ============================
            // USER INFO
            // ============================

            DateTimeFormatter dateFormatter =
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy");

            DateTimeFormatter generatedFormatter =
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy, hh:mm a");

            document.add(
                    new Paragraph(
                            "User: "
                                    + safe(user.getName()),
                            normalFont));

            document.add(
                    new Paragraph(
                            "Email: "
                                    + safe(user.getEmail()),
                            normalFont));

            document.add(
                    new Paragraph(
                            "Report Period: "
                                    + startDate.format(
                                            dateFormatter)
                                    + " - "
                                    + endDate.format(
                                            dateFormatter),
                            normalFont));

            document.add(
                    new Paragraph(
                            "Generated On: "
                                    + LocalDateTime.now()
                                            .format(
                                                    generatedFormatter),
                            normalFont));

            document.add(
                    new Paragraph(" "));

            // ============================
            // FINANCIAL OVERVIEW
            // ============================

            document.add(
                    new Paragraph(
                            "Financial Overview",
                            headingFont));

            document.add(
                    new Paragraph(" "));

            PdfPTable overviewTable =
                    new PdfPTable(2);

            overviewTable.setWidthPercentage(100);

            addRow(
                    overviewTable,
                    "Monthly Income",
                    money(monthlyIncome),
                    boldFont,
                    normalFont);

            addRow(
                    overviewTable,
                    "Monthly Spending Limit",
                    money(monthlyLimit),
                    boldFont,
                    normalFont);

            addRow(
                    overviewTable,
                    "Planned Monthly Savings",
                    money(plannedSavings),
                    boldFont,
                    normalFont);

            addRow(
                    overviewTable,
                    "Report Period Spending",
                    money(totalSpending),
                    boldFont,
                    normalFont);

            addRow(
                    overviewTable,
                    "Total Transactions",
                    String.valueOf(
                            expenses.size()),
                    boldFont,
                    normalFont);

            document.add(overviewTable);

            document.add(
                    new Paragraph(" "));

            // ============================
            // LIMITS
            // ============================

            document.add(
                    new Paragraph(
                            "Configured Budget Limits",
                            headingFont));

            document.add(
                    new Paragraph(" "));

            PdfPTable limitsTable =
                    new PdfPTable(2);

            limitsTable.setWidthPercentage(100);

            addRow(
                    limitsTable,
                    "Daily Limit",
                    money(dailyLimit),
                    boldFont,
                    normalFont);

            addRow(
                    limitsTable,
                    "Weekly Limit",
                    money(weeklyLimit),
                    boldFont,
                    normalFont);

            addRow(
                    limitsTable,
                    "15-Day Limit",
                    money(fifteenDayLimit),
                    boldFont,
                    normalFont);

            addRow(
                    limitsTable,
                    "Monthly Limit",
                    money(monthlyLimit),
                    boldFont,
                    normalFont);

            document.add(limitsTable);

            document.add(
                    new Paragraph(" "));

            // ============================
            // CATEGORY SPENDING
            // ============================

            document.add(
                    new Paragraph(
                            "Category-wise Spending",
                            headingFont));

            document.add(
                    new Paragraph(" "));

            if (categoryTotals.isEmpty()) {

                document.add(
                        new Paragraph(
                                "No expenses recorded during this period.",
                                normalFont));

            } else {

                PdfPTable categoryTable =
                        new PdfPTable(3);

                categoryTable.setWidthPercentage(100);

                addHeader(
                        categoryTable,
                        "Category",
                        boldFont);

                addHeader(
                        categoryTable,
                        "Amount",
                        boldFont);

                addHeader(
                        categoryTable,
                        "Share",
                        boldFont);

                for (Map.Entry<String, Double> entry
                        : categoryTotals.entrySet()) {

                    double percentage =
                            totalSpending > 0
                                    ? (entry.getValue()
                                    / totalSpending) * 100
                                    : 0;

                    addCell(
                            categoryTable,
                            entry.getKey(),
                            normalFont);

                    addCell(
                            categoryTable,
                            money(entry.getValue()),
                            normalFont);

                    addCell(
                            categoryTable,
                            String.format(
                                    "%.1f%%",
                                    percentage),
                            normalFont);
                }

                document.add(categoryTable);
            }

            document.add(
                    new Paragraph(" "));

            // ============================
            // TRANSACTIONS
            // ============================

            document.add(
                    new Paragraph(
                            "Transactions",
                            headingFont));

            document.add(
                    new Paragraph(" "));

            if (expenses.isEmpty()) {

                document.add(
                        new Paragraph(
                                "No transactions recorded during this period.",
                                normalFont));

            } else {

                PdfPTable transactionTable =
                        new PdfPTable(6);

                transactionTable.setWidthPercentage(100);

                transactionTable.setWidths(
                        new float[]{
                                1.3f,
                                1.2f,
                                1.4f,
                                1.5f,
                                1.1f,
                                2.4f
                        });

                addHeader(
                        transactionTable,
                        "Date",
                        boldFont);

                addHeader(
                        transactionTable,
                        "Amount",
                        boldFont);

                addHeader(
                        transactionTable,
                        "Category",
                        boldFont);

                addHeader(
                        transactionTable,
                        "Merchant",
                        boldFont);

                addHeader(
                        transactionTable,
                        "Source",
                        boldFont);

                addHeader(
                        transactionTable,
                        "Details",
                        boldFont);

                DateTimeFormatter transactionFormatter =
                        DateTimeFormatter.ofPattern(
                                "dd MMM\nhh:mm a");

                for (Expense expense : expenses) {

                    String date = "-";

                    if (expense.getDateTime() != null) {

                        date =
                                expense.getDateTime()
                                        .format(
                                                transactionFormatter);
                    }

                    String category =
                            safe(
                                    expense.getCategory());

                    if (category.isBlank()) {
                        category = "Other";
                    }

                    String merchant =
                            safe(
                                    expense.getMerchant());

                    if (merchant.isBlank()) {
                        merchant = "-";
                    }

                    String source =
                            safe(
                                    expense.getSource());

                    if (source.isBlank()) {
                        source = "-";
                    }

                    String details =
                            safe(
                                    expense.getReason());

                    if (details.isBlank()) {

                        details =
                                safe(
                                        expense.getDescription());
                    }

                    if (details.isBlank()) {
                        details = "-";
                    }

                    addCell(
                            transactionTable,
                            date,
                            normalFont);

                    addCell(
                            transactionTable,
                            money(
                                    expense.getAmount()),
                            normalFont);

                    addCell(
                            transactionTable,
                            category,
                            normalFont);

                    addCell(
                            transactionTable,
                            merchant,
                            normalFont);

                    addCell(
                            transactionTable,
                            source,
                            normalFont);

                    addCell(
                            transactionTable,
                            details,
                            normalFont);
                }

                document.add(
                        transactionTable);
            }

            document.add(
                    new Paragraph(" "));

            // ============================
            // SPENDIQ INSIGHT
            // ============================

            document.add(
                    new Paragraph(
                            "SpendIQ Insight",
                            headingFont));

            document.add(
                    new Paragraph(" "));

            String insight =
                    buildInsight(
                            totalSpending,
                            monthlyLimit,
                            plannedSavings,
                            categoryTotals);

            document.add(
                    new Paragraph(
                            insight,
                            normalFont));

            document.add(
                    new Paragraph(" "));

            Paragraph footer =
                    new Paragraph(
                            "Generated automatically by SpendIQ.",
                            subtitleFont);

            footer.setAlignment(
                    Element.ALIGN_CENTER);

            document.add(footer);

            document.close();

            return output.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate SpendIQ PDF report.",
                    e);
        }
    }

    // =========================================================
    // INSIGHT
    // =========================================================

    private String buildInsight(
            double periodSpending,
            double monthlyLimit,
            double plannedSavings,
            Map<String, Double> categoryTotals) {

        if (periodSpending <= 0) {

            return "No expenses were recorded during this report period. "
                    + "Your planned monthly savings are "
                    + money(plannedSavings)
                    + ".";
        }

        String topCategory = "Other";
        double topAmount = 0.0;

        for (Map.Entry<String, Double> entry
                : categoryTotals.entrySet()) {

            if (entry.getValue() > topAmount) {

                topCategory =
                        entry.getKey();

                topAmount =
                        entry.getValue();
            }
        }

        StringBuilder insight =
                new StringBuilder();

        insight.append(
                "You spent ")
                .append(
                        money(periodSpending))
                .append(
                        " during this report period.");

        if (topAmount > 0) {

            insight.append(
                    " Your highest spending category was ")
                    .append(topCategory)
                    .append(" at ")
                    .append(
                            money(topAmount))
                    .append(".");
        }

        if (monthlyLimit > 0) {

            double percentage =
                    (periodSpending
                            / monthlyLimit)
                            * 100;

            insight.append(
                    " This period's spending is approximately ")
                    .append(
                            String.format(
                                    "%.1f",
                                    percentage))
                    .append(
                            "% of your monthly spending limit.");
        }

        insight.append(
                " Your planned monthly savings are ")
                .append(
                        money(plannedSavings))
                .append(".");

        return insight.toString();
    }

    // =========================================================
    // PDF HELPERS
    // =========================================================

    private void addRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {

        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(
                                label,
                                labelFont));

        labelCell.setPadding(7);

        table.addCell(labelCell);

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                value,
                                valueFont));

        valueCell.setPadding(7);

        table.addCell(valueCell);
    }

    private void addHeader(
            PdfPTable table,
            String text,
            Font font) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                font));

        cell.setPadding(6);

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER);

        table.addCell(cell);
    }

    private void addCell(
            PdfPTable table,
            String text,
            Font font) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                font));

        cell.setPadding(5);

        table.addCell(cell);
    }

    // =========================================================
    // MONEY FORMAT
    // =========================================================

    private String money(double amount) {

        return String.format(
                "INR %.2f",
                amount);
    }

    // =========================================================
    // NULL SAFE STRING
    // =========================================================

    private String safe(String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }
}