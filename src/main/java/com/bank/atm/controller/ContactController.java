package com.bank.atm.controller;

import com.bank.atm.dto.TrustedContactDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.TrustedContact;
import com.bank.atm.exception.BankingException;
import com.bank.atm.security.SecurityUtils;
import com.bank.atm.service.AccountService;
import com.bank.atm.service.TrustedContactService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/contacts")
public class ContactController {

    private final TrustedContactService trustedContactService;
    private final AccountService accountService;

    public ContactController(TrustedContactService trustedContactService, AccountService accountService) {
        this.trustedContactService = trustedContactService;
        this.accountService = accountService;
    }

    @GetMapping
    public String contactsPage(Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        BankAccount account = accountService.getAccountByUsername(username);
        List<TrustedContact> contacts = trustedContactService.getContacts(account);

        if (!model.containsAttribute("contactDto")) {
            model.addAttribute("contactDto", new TrustedContactDto());
        }
        model.addAttribute("account", account);
        model.addAttribute("contacts", contacts);

        return "customer/contacts";
    }

    @PostMapping("/add")
    public String handleAddContact(@Valid @ModelAttribute("contactDto") TrustedContactDto dto,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        if (bindingResult.hasErrors()) {
            BankAccount account = accountService.getAccountByUsername(username);
            List<TrustedContact> contacts = trustedContactService.getContacts(account);
            model.addAttribute("account", account);
            model.addAttribute("contacts", contacts);
            return "customer/contacts";
        }

        try {
            trustedContactService.addContact(username, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Trusted contact added successfully!");
            return "redirect:/contacts";
        } catch (BankingException ex) {
            BankAccount account = accountService.getAccountByUsername(username);
            List<TrustedContact> contacts = trustedContactService.getContacts(account);
            model.addAttribute("account", account);
            model.addAttribute("contacts", contacts);
            model.addAttribute("errorMessage", ex.getMessage());
            return "customer/contacts";
        }
    }

    @PostMapping("/{id}/delete")
    public String handleDeleteContact(@PathVariable("id") Long contactId, RedirectAttributes redirectAttributes) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        try {
            trustedContactService.deleteContact(username, contactId);
            redirectAttributes.addFlashAttribute("successMessage", "Trusted contact removed.");
        } catch (BankingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/contacts";
    }
}
