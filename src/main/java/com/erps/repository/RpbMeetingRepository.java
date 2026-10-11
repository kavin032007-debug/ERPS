package com.erps.repository;

import com.erps.entity.RpbMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RpbMeetingRepository extends JpaRepository<RpbMeeting, Long> {
    Optional<RpbMeeting> findByExamId(Long examId);
}
