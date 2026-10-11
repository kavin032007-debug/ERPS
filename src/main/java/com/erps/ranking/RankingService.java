package com.erps.ranking;

import com.erps.dto.StudentRankEntry;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for computing semester/batch rank lists using Java PriorityQueue with
 * multi-tier tie-breakers and standard competition ranking.
 */
@Service
public class RankingService {

    /**
     * Comparator:
     * 1. GPA descending
     * 2. Total marks descending
     * 3. Register number ascending (tie-breaker for deterministic queue order)
     */
    public static final Comparator<StudentRankEntry> RANK_COMPARATOR = (e1, e2) -> {
        int gpaCompare = e2.getGpa().compareTo(e1.getGpa());
        if (gpaCompare != 0) {
            return gpaCompare;
        }

        int marksCompare = e2.getTotalMarks().compareTo(e1.getTotalMarks());
        if (marksCompare != 0) {
            return marksCompare;
        }

        return e1.getRegisterNo().compareTo(e2.getRegisterNo());
    };

    /**
     * Assigns ranks to students based on GPA and total marks.
     * Standard competition ranking: ties share the same rank (e.g., 1, 2, 2, 4).
     *
     * @param entries list of student records
     * @param rankRequiresNoArrear if true, only students with 0 arrears receive ranks
     * @return sorted list of ranked students
     */
    public List<StudentRankEntry> computeRanks(List<StudentRankEntry> entries, boolean rankRequiresNoArrear) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        PriorityQueue<StudentRankEntry> queue = new PriorityQueue<>(RANK_COMPARATOR);
        List<StudentRankEntry> ineligible = new ArrayList<>();

        for (StudentRankEntry entry : entries) {
            if (rankRequiresNoArrear && entry.getArrearCount() > 0) {
                entry.setRank(null);
                ineligible.add(entry);
            } else {
                queue.add(entry);
            }
        }

        List<StudentRankEntry> rankedList = new ArrayList<>();
        StudentRankEntry prev = null;
        int currentPosition = 0;
        int currentAssignedRank = 0;

        while (!queue.isEmpty()) {
            StudentRankEntry current = queue.poll();
            currentPosition++;

            if (prev == null) {
                currentAssignedRank = 1;
            } else {
                boolean isTie = current.getGpa().compareTo(prev.getGpa()) == 0
                        && current.getTotalMarks().compareTo(prev.getTotalMarks()) == 0;
                if (!isTie) {
                    currentAssignedRank = currentPosition;
                }
            }

            current.setRank(currentAssignedRank);
            rankedList.add(current);
            prev = current;
        }

        // Append ineligible students with rank null
        rankedList.addAll(ineligible);
        return rankedList;
    }
}
