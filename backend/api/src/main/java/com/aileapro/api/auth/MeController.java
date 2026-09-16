package com.aileapro.api.auth;

import com.aileapro.api.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class MeController {
    private final AuthService auth;
    private final UserRepository users;

    public MeController(AuthService auth, UserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        var user = users.findById(principal.getId()).orElseThrow();
        return MeResponse.from(user, auth.rolesFor(principal.getId()));
    }
}
