package com.bank.atm.controller;

import com.bank.atm.dto.FraudAlertResolutionDto;
import com.bank.atm.entity.*;
import com.bank.atm.enums.*;
import com.bank.atm.exception.BankingException;
import com.bank.atm.repository.TransactionRepository;
import com.bank.atm.security.SecurityUtils;
import com.bank.atm.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AccountService accountService;
    private final FraudDetectionService fraudDetectionService;
    private final EmergencyMoneyService emergencyMoneyService;
    private final AuditLogService auditLogService;
    private final TransactionRepository transactionRepository;

    public AdminController(AdminService adminService,
                           AccountService accountService,
                           FraudDetectionService fraudDetectionService,
                           EmergencyMoneyService emergencyMoneyService,
                           AuditLogService auditLogService,
                           TransactionRepository transactionRepository) {
        this.adminService = adminService;
        this.accountService = accountService;
        this.fraudDetectionService = fraudDetectionService;
        this.emergencyMoneyService = emergencyMoneyService;
        this.auditLogService = auditLogService;
        this.transactionRepository = transactionRepository;
    }

    // ---------------- DASHBOARD ----------------
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        Map<String, Object> stats = adminService.getSystemStatistics();
        Page<FraudAlert> recentAlerts = fraudDetectionService.getAllAlerts(null, null, PageRequest.of(0, 5));
        Page<Transaction> recentTransactions = transactionRepository.findAdminFilteredTransactions(null, null, null, PageRequest.of(0, 5));

        model.addAttribute("stats", stats);
        model.addAttribute("recentAlerts", recentAlerts.getContent());
        model.addAttribute("recentTransactions", recentTransactions.getContent());
        return "admin/dashboard";
    }

    // ---------------- CUSTOMERS & ACCOUNTS ----------------
    @GetMapping("/customers")
    public String viewCustomers(@RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                Model model) {
        Page<BankAccount> accounts = adminService.getAllAccounts(PageRequest.of(page, size));
        model.addAttribute("accounts", accounts.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", accounts.getTotalPages());
        model.addAttribute("totalElements", accounts.getTotalElements());
        return "admin/customers";
    }

    // ---------------- TRANSACTIONS ----------------
    @GetMapping("/transactions")
    public String viewTransactions(
            @RequestParam(value = "type", required = false) TransactionType type,
            @RequestParam(value = "status", required = false) TransactionStatus status,
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        String filterAcc = (accountNumber != null && !accountNumber.isBlank()) ? accountNumber.trim() : null;
        Page<Transaction> txPage = transactionRepository.findAdminFilteredTransactions(type, status, filterAcc, PageRequest.of(page, size));

        model.addAttribute("transactions", txPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", txPage.getTotalPages());
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("transactionTypes", TransactionType.values());
        model.addAttribute("transactionStatuses", TransactionStatus.values());

        return "admin/transactions";
    }

    // ---------------- FRAUD ALERTS ----------------
    @GetMapping("/fraud-alerts")
    public String viewFraudAlerts(
            @RequestParam(value = "status", required = false) FraudAlertStatus status,
            @RequestParam(value = "severity", required = false) FraudAlertSeverity severity,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        Page<FraudAlert> alertsPage = fraudDetectionService.getAllAlerts(status, severity, PageRequest.of(page, size));

        model.addAttribute("alerts", alertsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", alertsPage.getTotalPages());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSeverity", severity);
        model.addAttribute("statuses", FraudAlertStatus.values());
        model.addAttribute("severities", FraudAlertSeverity.values());

        return "admin/fraud-alerts";
    }

    @PostMapping("/fraud-alerts/{code}/resolve")
    public String resolveFraudAlert(@PathVariable("code") String alertCode,
                                    @Valid @ModelAttribute("resolutionDto") FraudAlertResolutionDto dto,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        String adminUsername = SecurityUtils.getCurrentUsername().orElseThrow();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Resolution notes are required (min 5 chars).");
            return "redirect:/admin/fraud-alerts";
        }

        try {
            fraudDetectionService.resolveAlert(alertCode, dto.getStatus(), dto.getResolutionNotes(), adminUsername, dto.isFreezeAccount());
            redirectAttributes.addFlashAttribute("successMessage", "Fraud alert " + alertCode + " updated to " + dto.getStatus());
        } catch (BankingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/fraud-alerts";
    }

    // ---------------- TOGGLE FREEZE ----------------
    @PostMapping("/accounts/{number}/toggle-freeze")
    public String toggleAccountFreeze(@PathVariable("number") String accountNumber,
                                      @RequestParam(value = "reason", defaultValue = "Administrative review") String reason,
                                      RedirectAttributes redirectAttributes) {
        String adminUsername = SecurityUtils.getCurrentUsername().orElseThrow();
        try {
            accountService.adminToggleAccountFreeze(accountNumber, adminUsername, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Account " + accountNumber + " freeze state toggled successfully.");
        } catch (BankingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/customers";
    }

    // ---------------- EMERGENCY REQUESTS ----------------
    @GetMapping("/emergency-requests")
    public String viewEmergencyRequests(@RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "size", defaultValue = "10") int size,
                                        Model model) {
        Page<EmergencyRequest> reqPage = emergencyMoneyService.getAllRequests(PageRequest.of(page, size));
        model.addAttribute("requests", reqPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reqPage.getTotalPages());
        return "admin/emergency-requests";
    }

    // ---------------- AUDIT LOGS ----------------
    @GetMapping("/audit-logs")
    public String viewAuditLogs(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "action", required = false) AuditAction action,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            Model model) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Page<AuditLog> logPage = auditLogService.getAuditLogs(username, action, start, end, PageRequest.of(page, size));

        model.addAttribute("auditLogs", logPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logPage.getTotalPages());
        model.addAttribute("username", username);
        model.addAttribute("selectedAction", action);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("actions", AuditAction.values());

        return "admin/audit-logs";
    }

    // ---------------- SYSTEM STATISTICS ----------------
    @GetMapping("/statistics")
    public String viewStatistics(Model model) {
        Map<String, Object> stats = adminService.getSystemStatistics();
        model.addAttribute("stats", stats);
        return "admin/statistics";
    }
}
