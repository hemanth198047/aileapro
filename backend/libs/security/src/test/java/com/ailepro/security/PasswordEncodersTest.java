package com.ailepro.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncodersTest {
    @Test
    void argon2Matches() {
        PasswordEncoder encoder = PasswordEncoders.argon2();
        String hash = encoder.encode("password1");
        assertTrue(encoder.matches("password1", hash));
    }
}
