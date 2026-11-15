package com.bank.atm.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", message != null && !message.toString().isBlank() ? message : "An unexpected error occurred.");
        if (exception != null) {
            model.addAttribute("exceptionType", exception.getClass().getSimpleName());
        }

        if (statusCode == 403) {
            return "error/403";
        } else if (statusCode == 404) {
            return "error/404";
        }
        return "error/error";
    }

    @GetMapping("/error/403")
    public String accessDenied(Model model) {
        model.addAttribute("statusCode", 403);
        model.addAttribute("errorMessage", "Access Denied. You do not have permission to view this banking resource.");
        return "error/403";
    }
}
