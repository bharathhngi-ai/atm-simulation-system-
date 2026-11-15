package com.bank.atm.security;

import com.bank.atm.service.FraudDetectionService;
import com.bank.atm.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UserService userService;
    private final FraudDetectionService fraudDetectionService;

    public CustomAuthenticationFailureHandler(@Lazy UserService userService,
                                              @Lazy FraudDetectionService fraudDetectionService) {
        this.userService = userService;
        this.fraudDetectionService = fraudDetectionService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");

        if (username != null && !username.isBlank()) {
            userService.recordFailedLogin(username, fraudDetectionService);
        }

        String errorMessage = "Invalid username or password.";
        if (exception instanceof LockedException) {
            errorMessage = "Your account has been locked due to repeated security failures. Please contact support.";
        }

        request.getSession().setAttribute("LOGIN_ERROR_MESSAGE", errorMessage);
        setDefaultFailureUrl("/login?error=true");
        super.onAuthenticationFailure(request, response, exception);
    }
}
