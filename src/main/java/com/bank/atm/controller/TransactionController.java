package com.bank.atm.controller;

import com.bank.atm.dto.DepositRequestDto;
import com.bank.atm.dto.TransferRequestDto;
import com.bank.atm.dto.WithdrawRequestDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.Transaction;
import com.bank.atm.entity.TrustedContact;
import com.bank.atm.entity.User;
import com.bank.atm.enums.TransactionType;
import com.bank.atm.exception.BankingException;
import com.bank.atm.security.SecurityUtils;
import com.bank.atm.service.AccountService;
import com.bank.atm.service.TransactionService;
import com.bank.atm.service.TrustedContactService;
import com.bank.atm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final UserService userService;
    private final TrustedContactService trustedContactService;

    public TransactionController(TransactionService transactionService,
                                 AccountService accountService,
                                 UserService userService,
                                 TrustedContactService trustedContactService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.userService = userService;
        this.trustedContactService = trustedContactService;
    }

    // ---------------- DEPOSIT ----------------
    @GetMapping("/deposit")
    public String depositPage(Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        BankAccount account = accountService.getAccountByUsername(username);

        DepositRequestDto dto = new DepositRequestDto();
        dto.setIdempotencyKey(UUID.randomUUID().toString());

        model.addAttribute("depositDto", dto);
        model.addAttribute("account", account);
        return "customer/deposit";
    }

    @PostMapping("/deposit")
    public String handleDeposit(@Valid @ModelAttribute("depositDto") DepositRequestDto dto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        if (bindingResult.hasErrors()) {
            BankAccount account = accountService.getAccountByUsername(username);
            model.addAttribute("account", account);
            return "customer/deposit";
        }

        try {
            Transaction tx = transactionService.deposit(username, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Deposit successful! Ref: " + tx.getReferenceNumber());
            return "redirect:/transactions/receipt/" + tx.getId();
        } catch (BankingException ex) {
            BankAccount account = accountService.getAccountByUsername(username);
            model.addAttribute("account", account);
            model.addAttribute("errorMessage", ex.getMessage());
            return "customer/deposit";
        }
    }

    // ---------------- WITHDRAWAL ----------------
    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        BankAccount account = accountService.getAccountByUsername(username);

        WithdrawRequestDto dto = new WithdrawRequestDto();
        dto.setIdempotencyKey(UUID.randomUUID().toString());

        model.addAttribute("withdrawDto", dto);
        model.addAttribute("account", account);
        return "customer/withdraw";
    }

    @PostMapping("/withdraw")
    public String handleWithdraw(@Valid @ModelAttribute("withdrawDto") WithdrawRequestDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        if (bindingResult.hasErrors()) {
            BankAccount account = accountService.getAccountByUsername(username);
            model.addAttribute("account", account);
            return "customer/withdraw";
        }

        try {
            Transaction tx = transactionService.withdraw(username, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Cash dispensed successfully! Ref: " + tx.getReferenceNumber());
            return "redirect:/transactions/receipt/" + tx.getId();
        } catch (BankingException ex) {
            BankAccount account = accountService.getAccountByUsername(username);
            model.addAttribute("account", account);
            model.addAttribute("errorMessage", ex.getMessage());
            return "customer/withdraw";
        }
    }

    // ---------------- TRANSFER ----------------
    @GetMapping("/transfer")
    public String transferPage(@RequestParam(value = "to", required = false) String toAccount, Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        BankAccount account = accountService.getAccountByUsername(username);
        List<TrustedContact> contacts = trustedContactService.getContacts(account);

        TransferRequestDto dto = new TransferRequestDto();
        dto.setIdempotencyKey(UUID.randomUUID().toString());
        if (toAccount != null && !toAccount.isBlank()) {
            dto.setRecipientAccountNumber(toAccount);
        }

        model.addAttribute("transferDto", dto);
        model.addAttribute("account", account);
        model.addAttribute("contacts", contacts);
        return "customer/transfer";
    }

    @PostMapping("/transfer")
    public String handleTransfer(@Valid @ModelAttribute("transferDto") TransferRequestDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        if (bindingResult.hasErrors()) {
            BankAccount account = accountService.getAccountByUsername(username);
            List<TrustedContact> contacts = trustedContactService.getContacts(account);
            model.addAttribute("account", account);
            model.addAttribute("contacts", contacts);
            return "customer/transfer";
        }

        try {
            Transaction tx = transactionService.transfer(username, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer completed successfully! Ref: " + tx.getReferenceNumber());
            return "redirect:/transactions/receipt/" + tx.getId();
        } catch (BankingException ex) {
            BankAccount account = accountService.getAccountByUsername(username);
            List<TrustedContact> contacts = trustedContactService.getContacts(account);
            model.addAttribute("account", account);
            model.addAttribute("contacts", contacts);
            model.addAttribute("errorMessage", ex.getMessage());
            return "customer/transfer";
        }
    }

    // ---------------- HISTORY ----------------
    @GetMapping("/history")
    public String transactionHistory(
            @RequestParam(value = "type", required = false) TransactionType type,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        BankAccount account = accountService.getAccountByUsername(username);

        Page<Transaction> transactionPage = transactionService.getTransactionHistory(
                account, type, startDate, endDate, PageRequest.of(page, size));

        model.addAttribute("account", account);
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        model.addAttribute("totalElements", transactionPage.getTotalElements());
        model.addAttribute("selectedType", type);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("transactionTypes", TransactionType.values());

        return "customer/history";
    }

    // ---------------- RECEIPT ----------------
    @GetMapping("/receipt/{id}")
    public String transactionReceipt(@PathVariable("id") Long id, Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        User currentUser = userService.getUserByUsername(username);
        Transaction tx = transactionService.getTransactionById(id);

        boolean isAuthorized = tx.getAccount().getUser().getId().equals(currentUser.getId()) ||
                (tx.getTargetAccount() != null && tx.getTargetAccount().getUser().getId().equals(currentUser.getId())) ||
                currentUser.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));

        if (!isAuthorized) {
            return "redirect:/error/403";
        }

        model.addAttribute("transaction", tx);
        model.addAttribute("account", tx.getAccount());
        model.addAttribute("user", currentUser);
        return "customer/receipt";
    }
}
