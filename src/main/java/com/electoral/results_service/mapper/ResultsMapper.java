package com.electoral.results_service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.electoral.results_service.dto.CandidateResult;
import com.electoral.results_service.dto.ResultsResponse;
import com.electoral.results_service.entity.Result;

@Component
public class ResultsMapper {

    public ResultsResponse toResponse(Long electionId, List<Result> results) {

        int totalVotes = results.stream()
                .mapToInt(Result::getVotes)
                .sum();

        List<CandidateResult> candidates = results.stream()
                .map(r -> new CandidateResult(
                        r.getCandidateName(),
                        r.getVotes()
                ))
                .toList();

        return ResultsResponse.builder()
                .electionId(electionId)
                .totalVotes(totalVotes)
                .candidates(candidates)
                .build();
    }
}