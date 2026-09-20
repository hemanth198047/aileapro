package com.aileapro.api.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Delivers transactional auth mail. When {@code spring.mail.host} is set, uses SMTP;
 * otherwise logs the message (dev/demo) so tokens are never silently dropped.
 */
@Component
public class AppMailer {

    private static final Logger log = LoggerFactory.getLogger(AppMailer.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String mailHost;

    public AppMailer(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${aileapro.mail.from:noreply@aileapro.local}") String from,
            @Value("${spring.mail.host:}") String mailHost) {
        this.mailSender = mailSender;
        this.from = from;
        this.mailHost = mailHost == null ? "" : mailHost.trim();
    }

    public boolean smtpConfigured() {
        return StringUtils.hasText(mailHost) && mailSender.getIfAvailable() != null;
    }

    public void send(String to, String subject, String body) {
        if (smtpConfigured()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.getObject().send(message);
            log.info("Sent mail to={} subject={}", to, subject);
            return;
        }
        log.info("MAIL (log-only) to={} subject={} body=\n{}", to, subject, body);
    }
}
