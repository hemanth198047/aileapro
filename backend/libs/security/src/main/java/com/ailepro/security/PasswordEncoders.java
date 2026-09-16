package com.ailepro.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.Security;

public final class PasswordEncoders {
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static PasswordEncoder argon2() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    private PasswordEncoders() {}
}
