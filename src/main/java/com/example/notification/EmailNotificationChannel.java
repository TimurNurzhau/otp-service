package com.example.notification;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.Properties;

/**
 * Канал отправки OTP-кодов по Email.
 * Использует SMTP сервер из email.properties.
 */
public class EmailNotificationChannel implements NotificationChannel {

    private final String username;
    private final String password;
    private final String fromEmail;
    private final Session session;

    public EmailNotificationChannel() {
        Properties config = loadConfig();
        this.username = config.getProperty("email.username");
        this.password = config.getProperty("email.password");
        this.fromEmail = config.getProperty("email.from");

        this.session = Session.getInstance(config, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private Properties loadConfig() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("email.properties")) {

            if (input == null) {
                System.err.println("[EMAIL] email.properties not found, using dummy config");
                return new Properties();
            }

            Properties props = new Properties();
            props.load(input);
            return props;

        } catch (Exception e) {
            System.err.println("[EMAIL] Failed to load config: " + e.getMessage());
            return new Properties();
        }
    }

    @Override
    public boolean send(String destination, String code) {
        // Если нет настроек — эмулируем отправку
        if (username == null || username.isEmpty()) {
            System.out.println("[EMAIL EMULATION] Sending code '" + code + "' to " + destination);
            return true;
        }

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(destination));
            message.setSubject("Your OTP Code");
            message.setText("Your verification code is: " + code);

            Transport.send(message);

            System.out.println("[EMAIL] Code sent to " + destination);
            return true;

        } catch (MessagingException e) {
            System.err.println("[EMAIL ERROR] Failed to send email: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getChannelName() {
        return "EMAIL";
    }
}