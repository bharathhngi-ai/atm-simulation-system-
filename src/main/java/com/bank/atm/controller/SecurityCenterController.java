package com.bank.atm.controller;

import com.bank.atm.dto.FreezeAccountDto;
import com.bank.atm.dto.UnfreezeAccountDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.FraudAlert;
import com.bank.atm.entity.User;
import com.bank.atm.exception.BankingException;
import com.bank.atm.security.SecurityUtils;
import com.bank.atm.service.AccountService;
import com.bank.atm.service.FraudDetectionService;
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

import java.util.List;

@Controller
@RequestMapping("/security")
public class SecurityCenterController {

    private final AccountService accountService;
    private final UserService userService;
    private final FraudDetectionService fraudDetectionService;

    public SecurityCenterController(AccountService accountService,
                                    UserService userService,
                                    FraudDetectionService fraudDetectionService) {
        this.accountService = accountService;
        this.userService = userService;
        this.fraudDetectionService = fraudDetectionService;
    }

    @GetMapping("/settings")
    public String securitySettings(Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        User user = userService.getUserByUsername(username);
        BankAccount account = accountService.getAccountByUsername(username);
        List<FraudAlert> alerts = fraudDetectionService.getRecentAlertsForAccount(account);

        if (!model.containsAttribute("freezeDto")) {
            model.addAttribute("freezeDto", new FreezeAccountDto());
        }
        if (!model.containsAttribute("unfreezeDto")) {
            model.addAttribute("unfreezeDto", new UnfreezeAccountDto());
        }
        model.addAttribute("user", user);
        model.addAttribute("account", account);
        model.addAttribute("alerts", alerts);

        return "customer/security";
    }

    @PostMapping("/freeze")
    public String handleFreeze(@Valid @ModelAttribute("freezeDto") FreezeAccountDto dto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide a valid reason and PIN to freeze your account.");
            return "redirect:/security/settings";
        }

        try {
            accountService.freezeAccount(username, dto.getReason(), dto.getAtmPin());
            redirectAttributes.addFlashAttribute("successMessage", "Your account has been frozen. Outgoing transfers and withdrawals are now blocked.");
        } catch (BankingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/security/settings";
    }

    @PostMapping("/unfreeze")
    public String handleUnfreeze(@Valid @ModelAttribute("unfreezeDto") UnfreezeAccountDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "PIN confirmation is required to unfreeze.");
            return "redirect:/security/settings";
        }

        try {
            accountService.unfreezeAccount(username, dto.getAtmPin());
            redirectAttributes.addFlashAttribute("successMessage", "Account unfrozen successfully. Full banking access restored.");
        } catch (BankingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/security/settings";
    }

    @GetMapping("/alerts")
    public String securityAlertsPage(Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        BankAccount account = accountService.getAccountByUsername(username);
        List<FraudAlert> alerts = fraudDetectionService.getRecentAlertsForAccount(account);

        model.addAttribute("account", account);
        model.addAttribute("alerts", alerts);
        return "customer/fraud-alerts";
    }
}
