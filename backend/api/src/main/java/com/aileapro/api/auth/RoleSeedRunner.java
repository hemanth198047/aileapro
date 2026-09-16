package com.aileapro.api.auth;

import com.aileapro.api.RoleRepository;
import com.aileapro.domain.RoleCodes;
import com.aileapro.persistence.RoleEntity;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleSeedRunner implements ApplicationRunner {
    private final RoleRepository roles;

    public RoleSeedRunner(RoleRepository roles) {
        this.roles = roles;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensure(RoleCodes.LEARNER);
        ensure(RoleCodes.ADMIN);
    }

    private void ensure(String code) {
        if (roles.findById(code).isEmpty()) {
            RoleEntity role = new RoleEntity();
            role.setCode(code);
            roles.save(role);
        }
    }
}
