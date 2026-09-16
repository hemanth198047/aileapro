package com.ailepro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleEntity.PK.class)
public class UserRoleEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "role_code")
    private String roleCode;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public static class PK implements Serializable {
        private UUID userId;
        private String roleCode;

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return java.util.Objects.equals(userId, pk.userId)
                    && java.util.Objects.equals(roleCode, pk.roleCode);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, roleCode);
        }
    }
}
