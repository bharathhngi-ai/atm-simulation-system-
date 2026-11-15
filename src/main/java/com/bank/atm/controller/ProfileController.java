package com.bank.atm.controller;

import com.bank.atm.dto.ProfileUpdateDto;
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

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final AccountService accountService;

    public ProfileController(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    @GetMapping
    public String profilePage(Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        User user = userService.getUserByUsername(username);
        BankAccount account = accountService.getAccountByUsername(username);

        if (!model.containsAttribute("profileDto")) {
            ProfileUpdateDto dto = new ProfileUpdateDto();
            dto.setFullName(user.getFullName());
            dto.setEmail(user.getEmail());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setAddress(user.getAddress());
            model.addAttribute("profileDto", dto);
        }

        model.addAttribute("user", user);
        model.addAttribute("account", account);

        return "customer/profile";
    }

    @PostMapping("/update")
    public String handleUpdate(@Valid @ModelAttribute("profileDto") ProfileUpdateDto dto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        if (bindingResult.hasErrors()) {
            User user = userService.getUserByUsername(username);
            BankAccount account = accountService.getAccountByUsername(username);
            model.addAttribute("user", user);
            model.addAttribute("account", account);
            return "customer/profile";
        }

        try {
            userService.updateProfile(username, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully!");
            return "redirect:/profile";
        } catch (BankingException ex) {
            User user = userService.getUserByUsername(username);
            BankAccount account = accountService.getAccountByUsername(username);
            model.addAttribute("user", user);
            model.addAttribute("account", account);
            model.addAttribute("errorMessage", ex.getMessage());
            return "customer/profile";
        }
    }
}
