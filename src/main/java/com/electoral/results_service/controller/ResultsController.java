package com.electoral.results_service.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.electoral.results_service.dto.ResultsResponse;
import com.electoral.results_service.service.ResultsService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
@Validated
public class ResultsController {

    private final ResultsService service;

    @GetMapping
    @Operation(summary = "Get results by election id")
    public ResultsResponse getResults(
            @RequestParam
            @Pattern(regexp = "^[0-9]+$", message = "electionId must be numeric")
            String electionId
    ) {
        return service.getResults(Long.parseLong(electionId));
    }
}