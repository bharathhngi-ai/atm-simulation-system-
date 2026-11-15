package com.bank.atm.controller;

import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.Transaction;
import com.bank.atm.entity.User;
import com.bank.atm.security.SecurityUtils;
import com.bank.atm.service.AccountService;
import com.bank.atm.service.EmergencyMoneyService;
import com.bank.atm.service.NotificationService;
import com.bank.atm.service.TransactionService;
import com.bank.atm.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final EmergencyMoneyService emergencyMoneyService;
    private final NotificationService notificationService;

    public DashboardController(UserService userService,
                               AccountService accountService,
                               TransactionService transactionService,
                               EmergencyMoneyService emergencyMoneyService,
                               NotificationService notificationService) {
        this.userService = userService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.emergencyMoneyService = emergencyMoneyService;
        this.notificationService = notificationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new IllegalStateException("Not authenticated"));

        User user = userService.getUserByUsername(username);
        if (user.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }

        BankAccount account = accountService.getAccountByUsername(username);
        List<Transaction> recentTransactions = transactionService.getRecentTransactions(account);
        long pendingEmergency = emergencyMoneyService.getPendingReceivedCount(account);
        long unreadNotifications = notificationService.getUnreadCount(user);

        model.addAttribute("user", user);
        model.addAttribute("account", account);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("pendingEmergencyCount", pendingEmergency);
        model.addAttribute("unreadCount", unreadNotifications);

        return "customer/dashboard";
    }
}
