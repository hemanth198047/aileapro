package com.aileapro.api.auth;

import com.aileapro.api.UserRoleRepository;
import com.aileapro.api.platform.ApiException;
import com.aileapro.persistence.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final LoginRateLimiter limiter;
    private final UserRoleRepository userRoles;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService auth, LoginRateLimiter limiter, UserRoleRepository userRoles) {
        this.auth = auth;
        this.limiter = limiter;
        this.userRoles = userRoles;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("csrf", token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MeResponse register(
            @Valid @RequestBody RegisterRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        UserEntity user = auth.register(body.email(), body.password());
        establishSession(user, request, response);
        return MeResponse.from(user, auth.rolesFor(user.getId()));
    }

    @PostMapping("/login")
    public MeResponse login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (!limiter.tryConsume(request)) {
            throw ApiException.rateLimited();
        }
        UserEntity user = auth.authenticate(body.email(), body.password());
        establishSession(user, request, response);
        return MeResponse.from(user, auth.rolesFor(user.getId()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        contextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody TokenRequest body) {
        auth.verifyEmail(body.token());
    }

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgot(@Valid @RequestBody EmailRequest body) {
        auth.forgotPassword(body.email());
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetRequest body) {
        auth.resetPassword(body.token(), body.password());
    }

    private void establishSession(UserEntity user, HttpServletRequest request, HttpServletResponse response) {
        AileaproUserPrincipal principal = AileaproUserPrincipal.from(user, userRoles);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {}

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password
    ) {}

    public record TokenRequest(@NotBlank String token) {}

    public record EmailRequest(@NotBlank @Email String email) {}

    public record ResetRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {}
}
