package com.ailepro.api.auth;

import com.ailepro.api.AuditLogRepository;
import com.ailepro.api.EmailTokenRepository;
import com.ailepro.api.RoleRepository;
import com.ailepro.api.UserRepository;
import com.ailepro.api.UserRoleRepository;
import com.ailepro.api.platform.ApiException;
import com.ailepro.domain.RoleCodes;
import com.ailepro.persistence.AuditLogEntity;
import com.ailepro.persistence.EmailTokenEntity;
import com.ailepro.persistence.UserEntity;
import com.ailepro.persistence.UserRoleEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;
    private final EmailTokenRepository tokens;
    private final AuditLogRepository audits;
    private final PasswordEncoder passwords;

    public AuthService(
            UserRepository users,
            UserRoleRepository userRoles,
            RoleRepository roles,
            EmailTokenRepository tokens,
            AuditLogRepository audits,
            PasswordEncoder passwords
    ) {
        this.users = users;
        this.userRoles = userRoles;
        this.roles = roles;
        this.tokens = tokens;
        this.audits = audits;
        this.passwords = passwords;
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

        issueToken(user.getId(), "verify", 24);
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
        users.findByEmailIgnoreCase(normalizeEmail(email)).ifPresent(user -> issueToken(user.getId(), "reset", 2));
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

    private void issueToken(UUID userId, String purpose, int hours) {
        byte[] buf = new byte[32];
        RANDOM.nextBytes(buf);
        String raw = HexFormat.of().formatHex(buf);
        EmailTokenEntity token = new EmailTokenEntity();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setPurpose(purpose);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(hours, ChronoUnit.HOURS));
        tokens.save(token);
        log.info("DEV email token purpose={} user={} token={}", purpose, userId, raw);
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
