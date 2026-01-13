package com.scalaris.config;

import com.scalaris.auth.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailSenderConfig {

    private static final Logger log = LoggerFactory.getLogger(MailSenderConfig.class);

    @Bean
    public PasswordResetService.MailSender mailSender() {
        return (to, subject, body) -> {
            log.info("MAIL (mock) -> to={}, subject={}, body=\n{}", to, subject, body);
        };
    }
}
