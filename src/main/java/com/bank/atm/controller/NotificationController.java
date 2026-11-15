package com.bank.atm.controller;

import com.bank.atm.entity.Notification;
import com.bank.atm.entity.User;
import com.bank.atm.security.SecurityUtils;
import com.bank.atm.service.NotificationService;
import com.bank.atm.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public String notificationsPage(@RequestParam(value = "page", defaultValue = "0") int page,
                                    @RequestParam(value = "size", defaultValue = "10") int size,
                                    Model model) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        User user = userService.getUserByUsername(username);

        Page<Notification> notifPage = notificationService.getUserNotifications(user, PageRequest.of(page, size));

        model.addAttribute("notifications", notifPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifPage.getTotalPages());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "customer/notifications";
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable("id") Long id, @RequestParam(value = "redirect", required = false) String redirect) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        User user = userService.getUserByUsername(username);
        notificationService.markAsRead(id, user);

        if (redirect != null && !redirect.isBlank()) {
            return "redirect:" + redirect;
        }
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(RedirectAttributes redirectAttributes) {
        String username = SecurityUtils.getCurrentUsername().orElseThrow();
        User user = userService.getUserByUsername(username);
        notificationService.markAllAsRead(user);
        redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        return "redirect:/notifications";
    }
}
