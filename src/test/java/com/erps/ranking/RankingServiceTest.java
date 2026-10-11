package com.erps.ranking;

import com.erps.dto.StudentRankEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RankingServiceTest {

    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new RankingService();
    }

    @Test
    @DisplayName("Verify competition ranking with ties (1, 2, 2, 4) and tie-breaking")
    void testRankingWithTies() {
        List<StudentRankEntry> entries = new ArrayList<>();
        entries.add(new StudentRankEntry(1L, "24CS001", "Alice", BigDecimal.valueOf(9.50), BigDecimal.valueOf(570.00), 0));
        entries.add(new StudentRankEntry(2L, "24CS002", "Bob", BigDecimal.valueOf(9.00), BigDecimal.valueOf(540.00), 0));
        entries.add(new StudentRankEntry(3L, "24CS003", "Charlie", BigDecimal.valueOf(9.00), BigDecimal.valueOf(540.00), 0));
        entries.add(new StudentRankEntry(4L, "24CS004", "Diana", BigDecimal.valueOf(8.50), BigDecimal.valueOf(510.00), 0));

        List<StudentRankEntry> ranked = rankingService.computeRanks(entries, true);

        assertEquals(4, ranked.size());
        assertEquals("Alice", ranked.get(0).getStudentName());
        assertEquals(1, ranked.get(0).getRank());

        // Bob and Charlie tie for rank 2
        assertEquals(2, ranked.get(1).getRank());
        assertEquals(2, ranked.get(2).getRank());

        // Diana must be rank 4 (standard competition ranking: 1, 2, 2, 4)
        assertEquals("Diana", ranked.get(3).getStudentName());
        assertEquals(4, ranked.get(3).getRank());
    }

    @Test
    @DisplayName("Students with arrears are excluded from ranking when rankRequiresNoArrear is true")
    void testArrearExclusion() {
        List<StudentRankEntry> entries = new ArrayList<>();
        entries.add(new StudentRankEntry(1L, "24CS001", "Alice", BigDecimal.valueOf(9.50), BigDecimal.valueOf(570.00), 0));
        entries.add(new StudentRankEntry(2L, "24CS002", "Bob With Arrear", BigDecimal.valueOf(9.60), BigDecimal.valueOf(580.00), 1));
        entries.add(new StudentRankEntry(3L, "24CS003", "Charlie", BigDecimal.valueOf(8.50), BigDecimal.valueOf(510.00), 0));

        List<StudentRankEntry> ranked = rankingService.computeRanks(entries, true);

        assertEquals(3, ranked.size());
        // Alice should be rank 1
        assertEquals("Alice", ranked.get(0).getStudentName());
        assertEquals(1, ranked.get(0).getRank());

        // Charlie should be rank 2
        assertEquals("Charlie", ranked.get(1).getStudentName());
        assertEquals(2, ranked.get(1).getRank());

        // Bob With Arrear is excluded from ranks
        StudentRankEntry arrearStudent = ranked.stream()
                .filter(e -> "Bob With Arrear".equals(e.getStudentName()))
                .findFirst().orElseThrow();
        assertNull(arrearStudent.getRank(), "Student with arrear must not have an assigned rank");
    }

    @Test
    @DisplayName("Same GPA with different total marks is broken by total marks")
    void testTotalMarksTieBreaker() {
        List<StudentRankEntry> entries = new ArrayList<>();
        // Same GPA 8.50, but Frank has higher total marks (520 vs 510)
        entries.add(new StudentRankEntry(1L, "24CS001", "Edward", BigDecimal.valueOf(8.50), BigDecimal.valueOf(510.00), 0));
        entries.add(new StudentRankEntry(2L, "24CS002", "Frank", BigDecimal.valueOf(8.50), BigDecimal.valueOf(520.00), 0));

        List<StudentRankEntry> ranked = rankingService.computeRanks(entries, false);

        assertEquals("Frank", ranked.get(0).getStudentName());
        assertEquals(1, ranked.get(0).getRank());

        assertEquals("Edward", ranked.get(1).getStudentName());
        assertEquals(2, ranked.get(1).getRank());
    }
}
