package com.electoral.results_service.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HistoryResponse {

    private int totalElections;
    private long totalVotesCast;
    private List<HistoryEntry> elections;

    @Data
    @Builder
    public static class HistoryEntry {
        private Long   electionId;
        private String electionName;
        private String electionType;
        private String date;
        private int    totalVotes;
        private String winner;
        private double winnerPct;
    }
}