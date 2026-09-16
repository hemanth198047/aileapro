package com.aileapro.api.auth;

import com.aileapro.persistence.UserEntity;

import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        boolean emailVerified,
        List<String> roles
) {
    public static MeResponse from(UserEntity user, List<String> roles) {
        return new MeResponse(user.getId(), user.getEmail(), user.getEmailVerifiedAt() != null, roles);
    }
}
