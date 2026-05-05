package com.electoral.results_service.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ComparisonResponse {

    private List<String>           electionNames;
    private List<CandidateTimeline> candidates;
    
    @Data
    @Builder
    public static class CandidateTimeline {
        private String        name;
        private List<Integer> votes;  
        private List<Double>  percentages;
    }
}