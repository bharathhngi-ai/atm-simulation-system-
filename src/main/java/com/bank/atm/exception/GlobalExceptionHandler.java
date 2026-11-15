package com.bank.atm.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BankingException.class)
    public String handleBankingException(BankingException ex, HttpServletRequest request, RedirectAttributes redirectAttributes, Model model) {
        log.warn("Banking business exception on {}: {}", request.getRequestURI(), ex.getMessage());
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + referer;
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request, RedirectAttributes redirectAttributes, Model model) {
        String defaultMsg = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("Validation failed. Please check your inputs.");
        log.warn("Validation error on {}: {}", request.getRequestURI(), defaultMsg);
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", defaultMsg);
            return "redirect:" + referer;
        }
        model.addAttribute("errorMessage", defaultMsg);
        return "error/error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request, RedirectAttributes redirectAttributes, Model model) {
        log.error("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());
        String msg = "Database constraint violation. Duplicate entry or dependent record conflict.";
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", msg);
            return "redirect:" + referer;
        }
        model.addAttribute("errorMessage", msg);
        return "error/error";
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public String handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request, Model model) {
        log.debug("Static resource not found: {}", request.getRequestURI());
        model.addAttribute("requestUri", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled exception at " + request.getRequestURI(), ex);
        model.addAttribute("errorMessage", "An unexpected system error occurred: " + ex.getMessage());
        model.addAttribute("requestUri", request.getRequestURI());
        return "error/error";
    }
}
