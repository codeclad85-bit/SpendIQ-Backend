package com.expense.expense_tracker.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    // SpendIQ Gmail account from Windows environment variable
    @Value("${SPENDIQ_MAIL_USERNAME:}")
    private String username;

    // Gmail 16-character App Password from Windows environment variable
    @Value("${SPENDIQ_MAIL_APP_PASSWORD:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Gmail SMTP configuration
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        // Credentials
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        mailSender.setDefaultEncoding("UTF-8");

        Properties properties = mailSender.getJavaMailProperties();

        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", "true");

        // Gmail STARTTLS
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");

        // Timeouts
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");

        return mailSender;
    }
}