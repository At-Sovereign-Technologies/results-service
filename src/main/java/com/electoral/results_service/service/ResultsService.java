package com.electoral.results_service.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.electoral.results_service.cache.RedisCacheAdapter;
import com.electoral.results_service.dto.CandidateResult;
import com.electoral.results_service.dto.ResultsResponse;
import com.electoral.results_service.entity.Result;
import com.electoral.results_service.exception.ResourceNotFoundException;
import com.electoral.results_service.repository.ResultRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResultsService {

    private final ResultRepository repository;
    private final RedisCacheAdapter cache;

    private static final Logger log = LoggerFactory.getLogger(ResultsService.class);

    public ResultsResponse getResults(Long electionId) {

        String key = "results:" + electionId;

        Object cached = cache.get(key);
        if (cached != null) {
            log.info("CACHE HIT - electionId={}", electionId);
            return (ResultsResponse) cached;
        }

        log.info("CACHE MISS - querying DB - electionId={}", electionId);

        List<Result> results = repository.findByElectionId(electionId);

        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Results not found");
        }

        int total = results.stream()
                .mapToInt(Result::getVotes)
                .sum();

        List<CandidateResult> candidates = results.stream()
                .map(r -> new CandidateResult(r.getCandidateName(), r.getVotes()))
                .toList();

        ResultsResponse response = ResultsResponse.builder()
                .electionId(electionId)
                .totalVotes(total)
                .candidates(candidates)
                .build();

        cache.set(key, response);
        log.info("CACHE STORE - electionId={}", electionId);

        return response;
    }
}