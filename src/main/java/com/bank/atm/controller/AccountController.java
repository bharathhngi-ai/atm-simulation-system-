package com.bank.atm.controller;

import com.bank.atm.dto.PinChangeRequestDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.User;
import com.bank.atm.exception.BankingException;
import com.bank.atm.security.SecurityUtils;
import com.bank.atm.service.AccountService;
import com.bank.atm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;
    private final UserService userService;

    public AccountController(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

    @GetMapping("/balance")
    public String balanceInquiry(Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        User user = userService.getUserByUsername(username);
        BankAccount account = accountService.getAccountByUsername(username);
        BigDecimal currentBalance = accountService.getBalance(username);

        model.addAttribute("user", user);
        model.addAttribute("account", account);
        model.addAttribute("balance", currentBalance);

        return "customer/balance";
    }

    @GetMapping("/change-pin")
    public String changePinForm(Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        User user = userService.getUserByUsername(username);
        BankAccount account = accountService.getAccountByUsername(username);

        if (!model.containsAttribute("pinDto")) {
            model.addAttribute("pinDto", new PinChangeRequestDto());
        }
        model.addAttribute("user", user);
        model.addAttribute("account", account);

        return "customer/change-pin";
    }

    @PostMapping("/change-pin")
    public String handleChangePin(@Valid @ModelAttribute("pinDto") PinChangeRequestDto dto,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        if (bindingResult.hasErrors()) {
            User user = userService.getUserByUsername(username);
            BankAccount account = accountService.getAccountByUsername(username);
            model.addAttribute("user", user);
            model.addAttribute("account", account);
            return "customer/change-pin";
        }

        try {
            accountService.changePin(username, dto);
            redirectAttributes.addFlashAttribute("successMessage", "ATM PIN changed successfully!");
            return "redirect:/account/change-pin";
        } catch (BankingException ex) {
            User user = userService.getUserByUsername(username);
            BankAccount account = accountService.getAccountByUsername(username);
            model.addAttribute("user", user);
            model.addAttribute("account", account);
            model.addAttribute("errorMessage", ex.getMessage());
            return "customer/change-pin";
        }
    }
}
