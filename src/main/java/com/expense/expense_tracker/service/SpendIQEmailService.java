package com.expense.expense_tracker.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.expense.expense_tracker.model.User;
import com.expense.expense_tracker.repository.UserRepository;

import jakarta.mail.internet.MimeMessage;

@Service
public class SpendIQEmailService {

    private final JavaMailSender mailSender;
    private final SpendIQReportService reportService;
    private final UserRepository userRepository;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public SpendIQEmailService(
            JavaMailSender mailSender,
            SpendIQReportService reportService,
            UserRepository userRepository) {

        this.mailSender = mailSender;
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

    // =========================================================
    // SEND CURRENT PERIOD REPORT
    // =========================================================

    public void sendCurrentPeriodReport(Long userId) {

        User user = getUser(userId);

        validateEmail(user);

        byte[] pdf =
                reportService.generateCurrentPeriodReport(userId);

        LocalDate today = LocalDate.now();

        LocalDate startDate;
        LocalDate endDate;

        if (today.getDayOfMonth() <= 10) {

            startDate =
                    today.withDayOfMonth(1);

            endDate =
                    today.withDayOfMonth(
                            Math.min(
                                    10,
                                    today.lengthOfMonth()));

        } else if (today.getDayOfMonth() <= 20) {

            startDate =
                    today.withDayOfMonth(11);

            endDate =
                    today.withDayOfMonth(
                            Math.min(
                                    20,
                                    today.lengthOfMonth()));

        } else {

            startDate =
                    today.withDayOfMonth(21);

            endDate =
                    today.withDayOfMonth(
                            today.lengthOfMonth());
        }

        sendReportEmail(
                user,
                pdf,
                startDate,
                endDate);
    }

    // =========================================================
    // SEND CUSTOM PERIOD REPORT
    // =========================================================

    public void sendReport(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        User user = getUser(userId);

        validateEmail(user);

        byte[] pdf =
                reportService.generateReport(
                        userId,
                        startDate,
                        endDate);

        sendReportEmail(
                user,
                pdf,
                startDate,
                endDate);
    }

    // =========================================================
    // ACTUAL EMAIL SENDING
    // =========================================================

    private void sendReportEmail(
            User user,
            byte[] pdf,
            LocalDate startDate,
            LocalDate endDate) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8");

            helper.setFrom(senderEmail);

            helper.setTo(user.getEmail());

            helper.setSubject(
                    "SpendIQ Financial Report | "
                            + startDate
                            + " to "
                            + endDate);

            String userName =
                    user.getName() == null
                            || user.getName().isBlank()
                            ? "SpendIQ User"
                            : user.getName().trim();

            String emailBody =
                    "Hello "
                            + userName
                            + ",\n\n"
                            + "Your SpendIQ financial report is ready.\n\n"
                            + "Report Period: "
                            + startDate
                            + " to "
                            + endDate
                            + "\n\n"
                            + "The attached PDF includes your:\n"
                            + "- Spending summary\n"
                            + "- Budget limits\n"
                            + "- Planned savings\n"
                            + "- Category-wise spending\n"
                            + "- Transaction details\n"
                            + "- SpendIQ financial insight\n\n"
                            + "Use this report to understand where your money "
                            + "is going and stay aligned with your budget.\n\n"
                            + "Regards,\n"
                            + "SpendIQ\n"
                            + "Smart Expense Tracking & Budget Management";

            helper.setText(
                    emailBody,
                    false);

            String fileName =
                    "SpendIQ_Report_"
                            + startDate
                            + "_to_"
                            + endDate
                            + ".pdf";

            helper.addAttachment(
                    fileName,
                    () -> new java.io.ByteArrayInputStream(pdf));

            mailSender.send(message);

            System.out.println(
                    "SpendIQ report email sent successfully to: "
                            + user.getEmail());

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to send SpendIQ report email to "
                            + user.getEmail(),
                    e);
        }
    }

    // =========================================================
    // USER
    // =========================================================

    private User getUser(Long userId) {

        if (userId == null) {

            throw new IllegalArgumentException(
                    "User ID is required.");
        }

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found: "
                                        + userId));
    }

    // =========================================================
    // EMAIL VALIDATION
    // =========================================================

    private void validateEmail(User user) {

        if (user.getEmail() == null
                || user.getEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "User does not have a registered email address.");
        }

        if (senderEmail == null
                || senderEmail.isBlank()) {

            throw new IllegalStateException(
                    "SpendIQ sender email is not configured.");
        }
    }
}