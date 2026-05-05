package com.electoral.results_service.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrendsResponse {

    private List<String>  labels;
    private List<Integer> totalVotesPerElection;
    private String        participationTrend;
    private List<CandidateTrend> candidateTrends;

    @Data
    @Builder
    public static class CandidateTrend {
        private String        name;
        private List<Double>  percentages;
        private String        trend;
    }
}