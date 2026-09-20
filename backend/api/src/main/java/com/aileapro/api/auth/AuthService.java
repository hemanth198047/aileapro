package com.aileapro.api.auth;

import com.aileapro.api.AuditLogRepository;
import com.aileapro.api.EmailTokenRepository;
import com.aileapro.api.RoleRepository;
import com.aileapro.api.UserRepository;
import com.aileapro.api.UserRoleRepository;
import com.aileapro.api.mail.AppMailer;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.RoleCodes;
import com.aileapro.persistence.AuditLogEntity;
import com.aileapro.persistence.EmailTokenEntity;
import com.aileapro.persistence.UserEntity;
import com.aileapro.persistence.UserRoleEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;
    private final EmailTokenRepository tokens;
    private final AuditLogRepository audits;
    private final PasswordEncoder passwords;
    private final AppMailer mailer;
    private final String webOrigin;

    public AuthService(
            UserRepository users,
            UserRoleRepository userRoles,
            RoleRepository roles,
            EmailTokenRepository tokens,
            AuditLogRepository audits,
            PasswordEncoder passwords,
            AppMailer mailer,
            @Value("${aileapro.web.origin:http://localhost:3000}") String webOrigin
    ) {
        this.users = users;
        this.userRoles = userRoles;
        this.roles = roles;
        this.tokens = tokens;
        this.audits = audits;
        this.passwords = passwords;
        this.mailer = mailer;
        this.webOrigin = webOrigin.endsWith("/") ? webOrigin.substring(0, webOrigin.length() - 1) : webOrigin;
    }

    @Transactional
    public UserEntity register(String email, String password) {
        String normalized = normalizeEmail(email);
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw ApiException.conflict("Email already registered");
        }
        if (password == null || password.length() < 8) {
            throw ApiException.validation("Password must be at least 8 characters");
        }
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(normalized);
        user.setPasswordHash(passwords.encode(password));
        users.save(user);

        if (roles.findById(RoleCodes.LEARNER).isEmpty()) {
            throw new IllegalStateException("Role seed missing: learner");
        }
        UserRoleEntity ur = new UserRoleEntity();
        ur.setUserId(user.getId());
        ur.setRoleCode(RoleCodes.LEARNER);
        userRoles.save(ur);

        issueToken(user, "verify", 24);
        audit(user.getId(), "auth.register", null);
        return user;
    }

    public UserEntity authenticate(String email, String password) {
        UserEntity user = users.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ApiException(401, "unauthorized", "Invalid email or password"));
        if (user.getPasswordHash() == null || !passwords.matches(password, user.getPasswordHash())) {
            throw new ApiException(401, "unauthorized", "Invalid email or password");
        }
        if (!"active".equals(user.getStatus())) {
            throw new ApiException(403, "forbidden", "Account disabled");
        }
        audit(user.getId(), "auth.login", null);
        return user;
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailTokenEntity token = requireToken(rawToken, "verify");
        UserEntity user = users.findById(token.getUserId()).orElseThrow();
        user.setEmailVerifiedAt(Instant.now());
        users.save(user);
        token.setUsedAt(Instant.now());
        tokens.save(token);
    }

    @Transactional
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCase(normalizeEmail(email)).ifPresent(user -> issueToken(user, "reset", 2));
    }

    @Transactional
    public void resetPassword(String rawToken, String password) {
        if (password == null || password.length() < 8) {
            throw ApiException.validation("Password must be at least 8 characters");
        }
        EmailTokenEntity token = requireToken(rawToken, "reset");
        UserEntity user = users.findById(token.getUserId()).orElseThrow();
        user.setPasswordHash(passwords.encode(password));
        users.save(user);
        token.setUsedAt(Instant.now());
        tokens.save(token);
        audit(user.getId(), "auth.password_reset", null);
    }

    public List<String> rolesFor(UUID userId) {
        return userRoles.findByUserId(userId).stream().map(UserRoleEntity::getRoleCode).toList();
    }

    private EmailTokenEntity requireToken(String raw, String purpose) {
        EmailTokenEntity token = tokens.findByTokenHashAndPurpose(hash(raw), purpose)
                .orElseThrow(() -> ApiException.validation("Invalid or expired token"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.validation("Invalid or expired token");
        }
        return token;
    }

    private void issueToken(UserEntity user, String purpose, int hours) {
        byte[] buf = new byte[32];
        RANDOM.nextBytes(buf);
        String raw = HexFormat.of().formatHex(buf);
        EmailTokenEntity token = new EmailTokenEntity();
        token.setId(UUID.randomUUID());
        token.setUserId(user.getId());
        token.setPurpose(purpose);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(hours, ChronoUnit.HOURS));
        tokens.save(token);

        if ("verify".equals(purpose)) {
            mailer.send(
                    user.getEmail(),
                    "Verify your AILEAPRO email",
                    "Welcome to AILEAPRO.\n\nVerify your email with this token (expires in "
                            + hours + " hours):\n" + raw
                            + "\n\nOr open: " + webOrigin + "/login?verify=" + raw
                            + "\n\nIf you did not create an account, ignore this message.\n");
        } else if ("reset".equals(purpose)) {
            mailer.send(
                    user.getEmail(),
                    "Reset your AILEAPRO password",
                    "A password reset was requested for your AILEAPRO account.\n\n"
                            + "Reset token (expires in " + hours + " hours):\n" + raw
                            + "\n\nOr open: " + webOrigin + "/login?reset=" + raw
                            + "\n\nIf you did not request this, ignore this message.\n");
        }
    }

    private void audit(UUID actor, String action, String ip) {
        AuditLogEntity row = new AuditLogEntity();
        row.setId(UUID.randomUUID());
        row.setActorId(actor);
        row.setAction(action);
        row.setIp(ip);
        audits.save(row);
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw ApiException.validation("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
