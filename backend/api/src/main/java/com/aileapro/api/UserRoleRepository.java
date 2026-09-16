package com.aileapro.api;

import com.aileapro.persistence.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleEntity.PK> {
    List<UserRoleEntity> findByUserId(UUID userId);
}
