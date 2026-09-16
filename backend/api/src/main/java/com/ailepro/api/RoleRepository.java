package com.ailepro.api;

import com.ailepro.persistence.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {}
