package com.bank.atm.controller;

import com.bank.atm.dto.RegisterRequestDto;
import com.bank.atm.entity.User;
import com.bank.atm.exception.BankingException;
import com.bank.atm.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            HttpServletRequest request,
                            Model model) {
        if (error != null) {
            HttpSession session = request.getSession(false);
            String errorMsg = "Invalid username or password.";
            if (session != null && session.getAttribute("LOGIN_ERROR_MESSAGE") != null) {
                errorMsg = (String) session.getAttribute("LOGIN_ERROR_MESSAGE");
                session.removeAttribute("LOGIN_ERROR_MESSAGE");
            }
            model.addAttribute("errorMessage", errorMsg);
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out safely.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterRequestDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerDto") RegisterRequestDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            User newUser = userService.registerCustomer(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account registered successfully! Your Account Number is " + newUser.getBankAccount().getAccountNumber() + ". Please login now.");
            return "redirect:/login";
        } catch (BankingException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
    }
}
