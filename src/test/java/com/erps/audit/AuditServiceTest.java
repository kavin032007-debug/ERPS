package com.erps.audit;

import com.erps.entity.AuditLog;
import com.erps.repository.AuditLogRepository;
import com.erps.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private AuditLogRepository auditLogRepository;
    private UserRepository userRepository;
    private AuditService auditService;
    private List<AuditLog> logStore;

    @BeforeEach
    void setUp() {
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        auditService = new AuditService(auditLogRepository, userRepository);
        logStore = new ArrayList<>();

        when(auditLogRepository.findTopByOrderByIdDesc()).thenAnswer(inv ->
                logStore.isEmpty() ? Optional.empty() : Optional.of(logStore.get(logStore.size() - 1))
        );

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog log = inv.getArgument(0);
            logStore.add(log);
            return log;
        });

        when(auditLogRepository.findAll()).thenAnswer(inv -> new ArrayList<>(logStore));
    }

    @Test
    @DisplayName("Hash chain connects sequential audit logs and passes integrity check")
    void testAuditHashChainIntegrity() {
        auditService.logAction("USER_LOGIN", "User", "1", null, "SUCCESS");
        auditService.logAction("MARKS_ENTERED", "Marks", "101", null, "CIA: 38");
        auditService.logAction("RESULT_APPROVED", "Examination", "1", "VERIFIED", "RPB_APPROVED");

        assertEquals(3, logStore.size());

        // Check hash linkage
        assertEquals("GENESIS_HASH_00000000000000000000000000000000000000000000000000000000", logStore.get(0).getPrevHash());
        assertEquals(logStore.get(0).getHash(), logStore.get(1).getPrevHash());
        assertEquals(logStore.get(1).getHash(), logStore.get(2).getPrevHash());

        // Chain integrity verified
        assertTrue(auditService.verifyIntegrity());
    }

    @Test
    @DisplayName("Tampering with a log entry causes integrity check to fail")
    void testTamperingDetection() {
        auditService.logAction("USER_LOGIN", "User", "1", null, "SUCCESS");
        auditService.logAction("MARKS_ENTERED", "Marks", "101", null, "CIA: 38");

        assertTrue(auditService.verifyIntegrity());

        // Tamper with marks entry
        logStore.get(1).setNewValue("CIA: 40"); // Unauthorized silent edit!

        assertFalse(auditService.verifyIntegrity(), "Integrity check must fail when log entry is tampered");
    }
}
