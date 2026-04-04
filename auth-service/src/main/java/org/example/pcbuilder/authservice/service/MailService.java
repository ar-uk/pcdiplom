package org.example.pcbuilder.authservice.service;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String mailUsername;

    public MailService(
            JavaMailSender mailSender,
            @Value("${auth.mail.from:}") String fromAddress,
            @Value("${spring.mail.username:}") String mailUsername
    ) {
        this.mailSender = mailSender;
        this.fromAddress = normalizeAddress(fromAddress);
        this.mailUsername = normalizeAddress(mailUsername);
    }

    public void sendTestEmail(String to) {
        sendMessage(to, "PC Builder auth-service email test", "This is a test email from auth-service. If you received this, Gmail SMTP is working.");
    }

    public void sendTwoFactorCode(String to, String code) {
        sendMessage(
                to,
                "Your PC Builder verification code",
                "Your verification code is: " + code + "\n\nIt expires soon. If you did not request this, ignore this email."
        );
    }

    private void sendMessage(String to, String subject, String body) {
        validateAddress(to, "recipient");
        SimpleMailMessage message = new SimpleMailMessage();
        String sender = !fromAddress.isBlank() ? fromAddress : mailUsername;
        if (!sender.isBlank()) {
            validateAddress(sender, "sender");
            message.setFrom(sender);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            Throwable rootCause = exception.getCause() != null ? exception.getCause() : exception;
            throw new IllegalStateException(
                    "SMTP send failed: " + rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage(),
                    exception
            );
        }
    }

    private static String normalizeAddress(String value) {
        return value == null ? "" : value.trim();
    }

    private static void validateAddress(String value, String label) {
        try {
            new InternetAddress(value, true).validate();
        } catch (AddressException exception) {
            throw new IllegalArgumentException("Invalid " + label + " email address: " + value, exception);
        }
    }
}