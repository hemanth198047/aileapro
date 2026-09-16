package com.ailepro.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class RoleEntity {
    @Id
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
