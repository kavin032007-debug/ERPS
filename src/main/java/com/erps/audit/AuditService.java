package com.erps.audit;

import com.erps.entity.AuditLog;
import com.erps.entity.Role;
import com.erps.entity.User;
import com.erps.repository.AuditLogRepository;
import com.erps.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Appends an immutable audit log entry linked to the previous log's SHA-256 hash.
     */
    @Transactional
    public synchronized AuditLog logAction(String action, String entity, String entityId,
                                           String oldValue, String newValue, String ipAddress) {
        String currentUserId = "SYSTEM";
        String currentRole = "SYSTEM";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            currentUserId = auth.getName();
            Optional<User> u = userRepository.findByEmail(currentUserId);
            if (u.isPresent()) {
                currentRole = u.get().getRole().name();
            }
        }

        // Get previous record for hash chaining
        Optional<AuditLog> latest = auditLogRepository.findTopByOrderByIdDesc();
        String prevHash = latest.map(AuditLog::getHash).orElse("GENESIS_HASH_00000000000000000000000000000000000000000000000000000000");

        LocalDateTime timestamp = LocalDateTime.now();
        String currentHash = computeHash(prevHash, timestamp, currentUserId, currentRole, action, entity, entityId, oldValue, newValue);

        AuditLog auditLog = new AuditLog(currentUserId, currentRole, ipAddress != null ? ipAddress : "127.0.0.1",
                action, entity, entityId, oldValue, newValue, prevHash, currentHash);
        auditLog.setTimestamp(timestamp);

        return auditLogRepository.save(auditLog);
    }

    public synchronized AuditLog logAction(String action, String entity, String entityId, String oldValue, String newValue) {
        return logAction(action, entity, entityId, oldValue, newValue, "127.0.0.1");
    }

    /**
     * Verifies cryptographic integrity of the entire audit log hash chain.
     * Returns true if 100% untampered, false if any entry has been altered or deleted.
     */
    @Transactional(readOnly = true)
    public boolean verifyIntegrity() {
        List<AuditLog> logs = auditLogRepository.findAll();
        if (logs.isEmpty()) {
            return true;
        }

        String expectedPrevHash = "GENESIS_HASH_00000000000000000000000000000000000000000000000000000000";

        for (AuditLog log : logs) {
            if (!expectedPrevHash.equals(log.getPrevHash())) {
                return false;
            }

            String recomputedHash = computeHash(log.getPrevHash(), log.getTimestamp(),
                    log.getUserId(), log.getRole(), log.getAction(), log.getEntity(),
                    log.getEntityId(), log.getOldValue(), log.getNewValue());

            if (!recomputedHash.equals(log.getHash())) {
                return false;
            }

            expectedPrevHash = log.getHash();
        }

        return true;
    }

    private String computeHash(String prevHash, LocalDateTime timestamp, String userId,
                               String role, String action, String entity, String entityId,
                               String oldValue, String newValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s",
                    prevHash,
                    timestamp != null ? timestamp.toString() : "",
                    userId != null ? userId : "",
                    role != null ? role : "",
                    action != null ? action : "",
                    entity != null ? entity : "",
                    entityId != null ? entityId : "",
                    oldValue != null ? oldValue : "",
                    newValue != null ? newValue : ""
            );
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }
}
