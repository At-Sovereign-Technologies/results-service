package com.electoral.results_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.electoral.results_service.dto.*;
import com.electoral.results_service.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Histórico, comparativas y tendencias electorales")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/history")
    @Operation(
        summary     = "Histórico de elecciones",
        description = "Lista todas las elecciones con su ganador y total de votos. " +
                      "Filtra por tipo y/o candidato."
    )
    public ResponseEntity<HistoryResponse> getHistory(

        @Parameter(description = "Tipo: PRESIDENTIAL, SENATE, LOCAL, REFERENDUM")
        @RequestParam(required = false) String type,

        @Parameter(description = "Nombre del candidato (búsqueda parcial)")
        @RequestParam(required = false) String candidate
    ) {
        return ResponseEntity.ok(analyticsService.getHistory(type, candidate));
    }

    @GetMapping("/comparison")
    @Operation(
        summary     = "Comparativa entre elecciones",
        description = "Compara votos y porcentajes de candidatos en 2 o más elecciones."
    )
    public ResponseEntity<ComparisonResponse> getComparison(

        @Parameter(description = "IDs de las elecciones a comparar (mínimo 2)", required = true)
        @RequestParam List<Long> electionIds
    ) {
        if (electionIds == null || electionIds.size() < 2) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(analyticsService.getComparison(electionIds));
    }

    @GetMapping("/trends")
    @Operation(
        summary     = "Tendencias electorales",
        description = "Calcula tendencia de participación y por candidato en un set de elecciones. " +
                      "Si no se pasan IDs, usa todas las elecciones disponibles."
    )
    public ResponseEntity<TrendsResponse> getTrends(

        @Parameter(description = "IDs en orden cronológico (opcional — default: todas)")
        @RequestParam(required = false) List<Long> electionIds
    ) {
        return ResponseEntity.ok(analyticsService.getTrends(electionIds));
    }
}