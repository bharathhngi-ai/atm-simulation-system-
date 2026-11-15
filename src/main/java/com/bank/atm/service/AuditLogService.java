package com.bank.atm.service;

import com.bank.atm.entity.AuditLog;
import com.bank.atm.enums.AuditAction;
import com.bank.atm.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String username, AuditAction action, String entityName, String entityId, String details, String status) {
        try {
            String ipAddress = "127.0.0.1";
            String userAgent = "Unknown";

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                    ipAddress = xForwardedFor.split(",")[0].trim();
                } else {
                    ipAddress = request.getRemoteAddr();
                }
                userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 250) {
                    userAgent = userAgent.substring(0, 250);
                }
            }

            AuditLog auditLog = new AuditLog(username, action, entityName, entityId, details, ipAddress, status);
            auditLog.setUserAgent(userAgent);
            auditLogRepository.save(auditLog);
            log.info("[AUDIT] user={}, action={}, entity={}, status={}", username, action, entityName, status);
        } catch (Exception ex) {
            log.error("Failed to write audit log: {}", ex.getMessage());
        }
    }

    public Page<AuditLog> getAuditLogs(String username, AuditAction action, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditLogRepository.findFilteredAuditLogs(
                username != null && !username.isBlank() ? username : null,
                action,
                start,
                end,
                pageable
        );
    }
}
