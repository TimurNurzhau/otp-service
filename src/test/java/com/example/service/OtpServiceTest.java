package com.example.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OtpServiceTest {

    private final OtpService otpService = new OtpService();

    @Test
    void testGetAvailableChannels() {
        String[] channels = otpService.getAvailableChannels();

        assertNotNull(channels);
        assertTrue(channels.length >= 4);

        // Проверяем, что все нужные каналы присутствуют
        boolean hasEmail = false;
        boolean hasSms = false;
        boolean hasTelegram = false;
        boolean hasFile = false;

        for (String ch : channels) {
            switch (ch.toLowerCase()) {
                case "email": hasEmail = true; break;
                case "sms": hasSms = true; break;
                case "telegram": hasTelegram = true; break;
                case "file": hasFile = true; break;
            }
        }

        assertTrue(hasEmail, "Email channel should be available");
        assertTrue(hasSms, "SMS channel should be available");
        assertTrue(hasTelegram, "Telegram channel should be available");
        assertTrue(hasFile, "File channel should be available");
    }

    @Test
    void testGenerateCodeLength() {
        // Проверяем, что generateCode работает корректно через рефлексию
        try {
            var method = OtpService.class.getDeclaredMethod("generateCode", int.class);
            method.setAccessible(true);

            String code4 = (String) method.invoke(otpService, 4);
            String code6 = (String) method.invoke(otpService, 6);
            String code8 = (String) method.invoke(otpService, 8);

            assertEquals(4, code4.length());
            assertEquals(6, code6.length());
            assertEquals(8, code8.length());

            // Проверяем, что код состоит только из цифр
            assertTrue(code4.matches("\\d+"));
            assertTrue(code6.matches("\\d+"));
            assertTrue(code8.matches("\\d+"));

        } catch (Exception e) {
            fail("Could not test generateCode: " + e.getMessage());
        }
    }
}