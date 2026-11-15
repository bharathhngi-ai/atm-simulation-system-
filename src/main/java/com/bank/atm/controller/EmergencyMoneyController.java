package com.bank.atm.controller;

import com.bank.atm.dto.EmergencyMoneyRequestDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.EmergencyRequest;
import com.bank.atm.entity.Transaction;
import com.bank.atm.entity.TrustedContact;
import com.bank.atm.exception.BankingException;
import com.bank.atm.security.SecurityUtils;
import com.bank.atm.service.AccountService;
import com.bank.atm.service.EmergencyMoneyService;
import com.bank.atm.service.TrustedContactService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/emergency")
public class EmergencyMoneyController {

    private final EmergencyMoneyService emergencyMoneyService;
    private final AccountService accountService;
    private final TrustedContactService trustedContactService;

    public EmergencyMoneyController(EmergencyMoneyService emergencyMoneyService,
                                    AccountService accountService,
                                    TrustedContactService trustedContactService) {
        this.emergencyMoneyService = emergencyMoneyService;
        this.accountService = accountService;
        this.trustedContactService = trustedContactService;
    }

    @GetMapping("/request")
    public String requestForm(@RequestParam(value = "to", required = false) String toAccount, Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        BankAccount account = accountService.getAccountByUsername(username);
        List<TrustedContact> contacts = trustedContactService.getContacts(account);
        List<EmergencyRequest> sentRequests = emergencyMoneyService.getSentRequests(account);

        EmergencyMoneyRequestDto dto = new EmergencyMoneyRequestDto();
        if (toAccount != null && !toAccount.isBlank()) {
            dto.setTargetAccountNumber(toAccount);
        }

        model.addAttribute("requestDto", dto);
        model.addAttribute("account", account);
        model.addAttribute("contacts", contacts);
        model.addAttribute("sentRequests", sentRequests);

        return "customer/emergency-request";
    }

    @PostMapping("/request")
    public String handleCreateRequest(@Valid @ModelAttribute("requestDto") EmergencyMoneyRequestDto dto,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        BankAccount account = accountService.getAccountByUsername(username);

        if (bindingResult.hasErrors()) {
            List<TrustedContact> contacts = trustedContactService.getContacts(account);
            List<EmergencyRequest> sentRequests = emergencyMoneyService.getSentRequests(account);
            model.addAttribute("account", account);
            model.addAttribute("contacts", contacts);
            model.addAttribute("sentRequests", sentRequests);
            return "customer/emergency-request";
        }

        try {
            EmergencyRequest req = emergencyMoneyService.createRequest(username, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Emergency money request of ₹" + req.getAmount() + " sent successfully to account " + dto.getTargetAccountNumber());
            return "redirect:/emergency/request";
        } catch (BankingException ex) {
            List<TrustedContact> contacts = trustedContactService.getContacts(account);
            List<EmergencyRequest> sentRequests = emergencyMoneyService.getSentRequests(account);
            model.addAttribute("account", account);
            model.addAttribute("contacts", contacts);
            model.addAttribute("sentRequests", sentRequests);
            model.addAttribute("errorMessage", ex.getMessage());
            return "customer/emergency-request";
        }
    }

    @GetMapping("/received")
    public String receivedRequestsPage(Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        BankAccount account = accountService.getAccountByUsername(username);
        List<EmergencyRequest> received = emergencyMoneyService.getReceivedRequests(account);

        model.addAttribute("account", account);
        model.addAttribute("receivedRequests", received);
        return "customer/emergency-received";
    }

    @PostMapping("/received/{id}/approve")
    public String handleApprove(@PathVariable("id") Long requestId,
                                @RequestParam("atmPin") String atmPin,
                                @RequestParam(value = "note", required = false) String note,
                                RedirectAttributes redirectAttributes) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        try {
            Transaction tx = emergencyMoneyService.approveRequest(username, requestId, atmPin, note);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Emergency request approved and funds transferred! Ref: " + tx.getReferenceNumber());
            return "redirect:/transactions/receipt/" + tx.getId();
        } catch (BankingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/emergency/received";
        }
    }

    @PostMapping("/received/{id}/reject")
    public String handleReject(@PathVariable("id") Long requestId,
                               @RequestParam(value = "reason", required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        try {
            emergencyMoneyService.rejectRequest(username, requestId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Emergency request was declined.");
            return "redirect:/emergency/received";
        } catch (BankingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/emergency/received";
        }
    }
}
