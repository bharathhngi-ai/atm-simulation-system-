package com.bank.atm.security;

import com.bank.atm.enums.AuditAction;
import com.bank.atm.service.AuditLogService;
import com.bank.atm.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public CustomAuthenticationSuccessHandler(@Lazy UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        userService.resetFailedLoginAttempts(username);
        auditLogService.logAction(username, AuditAction.USER_LOGIN_SUCCESS, "User", username, "Successful login", "SUCCESS");

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            setDefaultTargetUrl("/admin/dashboard");
        } else {
            setDefaultTargetUrl("/dashboard");
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
