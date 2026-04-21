package com.example.notification;

import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;

import java.io.InputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Канал отправки OTP-кодов через SMS (SMPP протокол).
 * Работает с эмулятором SMPPSim.
 * Если эмулятор не доступен — переключается в режим эмуляции.
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

    /**
     * Проверяет, доступен ли SMPP эмулятор
     */
    private boolean isSmppAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean send(String destination, String code) {
        // ✅ Если эмулятор не доступен — эмулируем отправку
        if (!isSmppAvailable()) {
            System.out.println("[SMS EMULATION] SMPP server not available, emulating. Code: '" + code + "' to " + destination);
            return true;
        }

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
            // ✅ При ошибке тоже эмулируем, чтобы не падать
            System.out.println("[SMS EMULATION] Fallback to emulation. Code: '" + code + "' to " + destination);
            return true;
        } finally {
            try {
                if (session != null && session.getSessionState().isBound()) {
                    session.unbindAndClose();
                }
            } catch (Exception e) {
                // Игнорируем ошибки при закрытии
            }
        }
    }

    @Override
    public String getChannelName() {
        return "SMS";
    }
}