package com.bizcord.backend.service;

import com.bizcord.backend.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Async
    public void sendWelcomeEmail(User user) {
        Context ctx = new Context();
        ctx.setVariable("username", user.getUsername2());
        ctx.setVariable("frontendUrl", frontendUrl);
        send(user.getEmail(), "Welcome to Bizcord!", "welcome-email", ctx);
    }

    @Async
    public void sendVerificationEmail(User user, String token) {
        String verifyUrl = frontendUrl + "/auth/verify-email?token=" + token;
        Context ctx = new Context();
        ctx.setVariable("username", user.getUsername2());
        ctx.setVariable("verifyUrl", verifyUrl);
        send(user.getEmail(), "Verify your Bizcord account", "verification-email", ctx);
    }

    @Async
    public void sendForgotPasswordEmail(User user, String token) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + token;
        Context ctx = new Context();
        ctx.setVariable("username", user.getUsername2());
        ctx.setVariable("resetUrl", resetUrl);
        send(user.getEmail(), "Reset your Bizcord password", "forgot-password-email", ctx);
    }

    @Async
    public void sendVerificationSuccessEmail(User user) {
        String loginUrl = frontendUrl + "/auth/login";
        Context ctx = new Context();
        ctx.setVariable("username", user.getUsername2());
        ctx.setVariable("loginUrl", loginUrl);
        send(user.getEmail(), "Your Bizcord account is verified!", "verification-success-email", ctx);
    }

    private void send(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
        }
    }
}
