package com.electoral.results_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CandidateResult {
    private String name;
    private Integer votes;
}