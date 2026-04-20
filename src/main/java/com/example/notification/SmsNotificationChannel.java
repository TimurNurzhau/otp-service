package com.example.notification;

import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Канал отправки OTP-кодов через SMS (SMPP протокол).
 * Работает с эмулятором SMPPSim.
 */
public class SmsNotificationChannel implements NotificationChannel {

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;

    public SmsNotificationChannel() {
        Properties config = loadConfig();
        this.host = config.getProperty("smpp.host", "localhost");
        this.port = Integer.parseInt(config.getProperty("smpp.port", "2775"));
        this.systemId = config.getProperty("smpp.system_id", "smppclient1");
        this.password = config.getProperty("smpp.password", "password");
        this.systemType = config.getProperty("smpp.system_type", "OTP");
        this.sourceAddress = config.getProperty("smpp.source_addr", "OTPService");
    }

    private Properties loadConfig() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("sms.properties")) {

            Properties props = new Properties();
            if (input != null) {
                props.load(input);
            }
            return props;

        } catch (Exception e) {
            return new Properties();
        }
    }

    @Override
    public boolean send(String destination, String code) {
        SMPPSession session = new SMPPSession();

        try {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );

            session.connectAndBind(host, port, bindParameter);

            String message = "Your OTP code: " + code;

            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    message.getBytes(StandardCharsets.UTF_8)
            );

            System.out.println("[SMS] Code sent to " + destination);
            return true;

        } catch (Exception e) {
            System.err.println("[SMS ERROR] Failed to send SMS to " + destination + ": " + e.getMessage());
            return false; // Исправлено: возвращаем false при ошибке
        } finally {
            if (session != null && session.getSessionState().isBound()) {
                session.unbindAndClose();
            }
        }
    }

    @Override
    public String getChannelName() {
        return "SMS";
    }
}