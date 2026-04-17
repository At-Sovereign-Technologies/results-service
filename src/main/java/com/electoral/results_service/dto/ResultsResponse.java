package com.electoral.results_service.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultsResponse {
    private Long electionId;
    private Integer totalVotes;
    private List<CandidateResult> candidates;
}