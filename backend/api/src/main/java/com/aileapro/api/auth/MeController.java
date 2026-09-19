package com.aileapro.api.auth;

import com.aileapro.api.UserRepository;
import com.aileapro.api.privacy.PrivacyService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1")
public class MeController {
    private final AuthService auth;
    private final UserRepository users;
    private final PrivacyService privacy;

    public MeController(AuthService auth, UserRepository users, PrivacyService privacy) {
        this.auth = auth;
        this.users = users;
        this.privacy = privacy;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        var user = users.findById(principal.getId()).orElseThrow();
        return MeResponse.from(user, auth.rolesFor(principal.getId()));
    }

    /** Machine-readable copy of the learner's account and learning data. */
    @GetMapping("/me/export")
    public Map<String, Object> export(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return privacy.export(principal.getId());
    }

    /**
     * Anonymize the account: scrub PII and free text, mark status deleted.
     * Login is refused afterward. Learning aggregates may remain without identifiers.
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public PrivacyService.AnonymizeResult delete(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return privacy.anonymize(principal.getId());
    }
}
