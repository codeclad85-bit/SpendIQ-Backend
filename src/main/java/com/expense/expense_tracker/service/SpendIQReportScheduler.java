package com.expense.expense_tracker.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.expense.expense_tracker.model.ReportDeliveryLog;
import com.expense.expense_tracker.model.User;
import com.expense.expense_tracker.repository.ReportDeliveryLogRepository;
import com.expense.expense_tracker.repository.UserRepository;

@Service
public class SpendIQReportScheduler {

    private final SpendIQEmailService emailService;
    private final UserRepository userRepository;
    private final ReportDeliveryLogRepository deliveryLogRepository;

    public SpendIQReportScheduler(
            SpendIQEmailService emailService,
            UserRepository userRepository,
            ReportDeliveryLogRepository deliveryLogRepository) {

        this.emailService = emailService;
        this.userRepository = userRepository;
        this.deliveryLogRepository = deliveryLogRepository;
    }

    // Runs every day at 8:00 AM India time.
    //
    // 11th -> report for 1st to 10th
    // 21st -> report for 11th to 20th
    // 1st  -> previous month's 21st to last day

    @Scheduled(
            cron = "0 0 8 * * *",
            zone = "Asia/Kolkata"
    )
    public void sendAutomaticReports() {

        LocalDate today = LocalDate.now();

        LocalDate startDate;
        LocalDate endDate;

        int day = today.getDayOfMonth();

        if (day == 11) {

            startDate = today.withDayOfMonth(1);
            endDate = today.withDayOfMonth(10);

        } else if (day == 21) {

            startDate = today.withDayOfMonth(11);
            endDate = today.withDayOfMonth(20);

        } else if (day == 1) {

            LocalDate previousMonth = today.minusMonths(1);

            startDate = previousMonth.withDayOfMonth(21);

            endDate = previousMonth.withDayOfMonth(
                    previousMonth.lengthOfMonth());

        } else {

            return;
        }

        List<User> users = userRepository.findAll();

        for (User user : users) {

            if (user.getId() == null) {
                continue;
            }

            if (user.getEmail() == null
                    || user.getEmail().isBlank()) {
                continue;
            }

            boolean alreadySent =
                    deliveryLogRepository
                            .existsByUserIdAndPeriodStartAndPeriodEnd(
                                    user.getId(),
                                    startDate,
                                    endDate);

            if (alreadySent) {

                System.out.println(
                        "SpendIQ report already sent | User: "
                                + user.getId()
                                + " | Period: "
                                + startDate
                                + " to "
                                + endDate);

                continue;
            }

            try {

                emailService.sendReport(
                        user.getId(),
                        startDate,
                        endDate);

                ReportDeliveryLog log =
                        new ReportDeliveryLog(
                                user.getId(),
                                startDate,
                                endDate,
                                "SENT",
                                LocalDateTime.now());

                deliveryLogRepository.save(log);

                System.out.println(
                        "SpendIQ automatic report sent | User: "
                                + user.getId()
                                + " | Period: "
                                + startDate
                                + " to "
                                + endDate);

            } catch (Exception e) {

                System.err.println(
                        "SpendIQ automatic report failed | User: "
                                + user.getId()
                                + " | Period: "
                                + startDate
                                + " to "
                                + endDate
                                + " | Error: "
                                + e.getMessage());
            }
        }
    }
}