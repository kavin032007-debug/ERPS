package com.erps.service;

import com.erps.audit.AuditService;
import com.erps.entity.*;
import com.erps.repository.DummyMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
public class DummyIdService {

    private final DummyMappingRepository dummyMappingRepository;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    public DummyIdService(DummyMappingRepository dummyMappingRepository, AuditService auditService) {
        this.dummyMappingRepository = dummyMappingRepository;
        this.auditService = auditService;
    }

    /**
     * Generates or retrieves unique dummy IDs for all students registered in a course examination.
     */
    @Transactional
    public List<DummyMapping> generateOrGetDummyMappings(Examination exam, Course course, List<Student> students) {
        List<DummyMapping> mappings = new ArrayList<>();
        Set<String> usedDummyIds = new HashSet<>();

        // Load existing mappings
        List<DummyMapping> existing = dummyMappingRepository.findByExamIdAndCourseId(exam.getId(), course.getId());
        Map<Long, DummyMapping> studentMap = new HashMap<>();
        for (DummyMapping dm : existing) {
            studentMap.put(dm.getStudent().getId(), dm);
            usedDummyIds.add(dm.getDummyId());
        }

        for (Student s : students) {
            if (studentMap.containsKey(s.getId())) {
                mappings.add(studentMap.get(s.getId()));
            } else {
                String dummyId = generateUniqueDummyId(exam.getId(), course.getCode(), usedDummyIds);
                usedDummyIds.add(dummyId);
                DummyMapping dm = new DummyMapping(exam, s, course, dummyId);
                mappings.add(dummyMappingRepository.save(dm));
            }
        }

        return mappings;
    }

    private String generateUniqueDummyId(Long examId, String courseCode, Set<String> usedIds) {
        String candidate;
        do {
            int num = 1000 + random.nextInt(9000);
            candidate = String.format("DM-%s-%04d", courseCode, num);
        } while (usedIds.contains(candidate));
        return candidate;
    }

    /**
     * Resolves dummy ID back to Student entity. De-masking is strictly audit-logged.
     */
    @Transactional(readOnly = true)
    public Optional<Student> deMask(Long examId, Long courseId, String dummyId, String userEmail, String userRole) {
        Optional<DummyMapping> mapping = dummyMappingRepository.findByExamIdAndCourseIdAndDummyId(examId, courseId, dummyId);
        if (mapping.isPresent()) {
            auditService.logAction("DEMASK_DUMMY_ID", "DummyMapping", mapping.get().getId().toString(),
                    dummyId, mapping.get().getStudent().getRegisterNo());
            return Optional.of(mapping.get().getStudent());
        }
        return Optional.empty();
    }

    public List<DummyMapping> getMappingsForExamAndCourse(Long examId, Long courseId) {
        return dummyMappingRepository.findByExamIdAndCourseId(examId, courseId);
    }
}
