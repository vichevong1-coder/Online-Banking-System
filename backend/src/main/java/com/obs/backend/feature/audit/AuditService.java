package com.obs.backend.feature.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(String actionType, String entityId, String entityType, String details) {
        logAction(null, null, actionType, entityId, entityType, details);
    }

    public void logAction(UUID actorId, String actorEmail, String actionType, String entityId, String entityType, String details) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setCreatedAt(OffsetDateTime.now());
        log.setActionType(actionType);
        log.setEntityId(entityId);
        log.setEntityType(entityType);
        log.setDetails(details);

        if (actorId != null) {
            log.setActorId(actorId);
        } else {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                    log.setActorId(UUID.fromString(auth.getName()));
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (actorEmail != null) {
            log.setActorEmail(actorEmail);
        }

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                log.setIpAddress(ip);
            }
        } catch (Exception e) {
            // ignore
        }

        auditLogRepository.save(log);
    }
}
