package com.electoral.results_service.controller;

// ============================================================
//  TIPO: Integración (MockMvc) — ResultsController
//  Atributos: Funcionalidad (EQ-2, EQ-3) | Compatibilidad (EQ-8)
//             Usabilidad (EQ-12) | Seguridad (EQ-19, EQ-20)
// ============================================================

import com.electoral.results_service.dto.CandidateResult;
import com.electoral.results_service.dto.ResultsResponse;
import com.electoral.results_service.exception.ResourceNotFoundException;
import com.electoral.results_service.service.ResultsService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResultsController.class)
@DisplayName("ResultsController — Integración MockMvc")
class ResultsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private ResultsService service;

    // C-01 | EQ-2 | Retorna 200 con todos los campos correctos
    @Test @DisplayName("C-01 | EQ-2 | Retorna 200 con resultados correctos para electionId válido")
    void should_return200_when_electionIdIsValid() throws Exception {
        ResultsResponse response = ResultsResponse.builder()
                .electionId(1L).totalVotes(11500000)
                .candidates(List.of(
                    new CandidateResult("Juan Pérez", 5200000),
                    new CandidateResult("María Gómez", 5100000),
                    new CandidateResult("Carlos Rodríguez", 1200000)))
                .build();

        when(service.getResults(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/results").param("electionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.electionId").value(1))
                .andExpect(jsonPath("$.totalVotes").value(11500000))
                .andExpect(jsonPath("$.candidates.length()").value(3));
    }

    // C-02 | EQ-3 | Doble Verdad: totalVotes coincide con suma de candidatos
    @Test @DisplayName("C-02 | EQ-3 | Doble Verdad: totalVotes coincide con suma de candidatos")
    void should_matchTotalWithCandidateSum_when_returningResults() throws Exception {
        ResultsResponse response = ResultsResponse.builder()
                .electionId(1L).totalVotes(11500000)
                .candidates(List.of(
                    new CandidateResult("Juan Pérez", 5200000),
                    new CandidateResult("María Gómez", 5100000),
                    new CandidateResult("Carlos Rodríguez", 1200000)))
                .build();

        when(service.getResults(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/results").param("electionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(5200000 + 5100000 + 1200000));
    }

    // C-03 | EQ-19 | Retorna 404 para elección inexistente
    @Test @DisplayName("C-03 | EQ-19 | Retorna 404 para elección inexistente sin exponer otros datos")
    void should_return404_when_electionNotFound() throws Exception {
        when(service.getResults(999L)).thenThrow(new ResourceNotFoundException("Results not found"));

        mockMvc.perform(get("/api/v1/results").param("electionId", "999"))
                .andExpect(status().isNotFound());
    }

    // U-01 | EQ-12 | Rechaza electionId con letras
    @Test @DisplayName("U-01 | EQ-12 | Rechaza electionId con letras")
    void should_return400_when_electionIdHasLetters() throws Exception {
        mockMvc.perform(get("/api/v1/results").param("electionId", "ABC"))
                .andExpect(status().isBadRequest());
    }

    // U-02 | EQ-12 | Rechaza electionId con caracteres especiales
    @Test @DisplayName("U-02 | EQ-12 | Rechaza electionId con caracteres especiales")
    void should_return400_when_electionIdHasSpecialChars() throws Exception {
        mockMvc.perform(get("/api/v1/results").param("electionId", "1-2"))
                .andExpect(status().isBadRequest());
    }

    // U-03 | EQ-12 | Rechaza cuando no se envía electionId
    @Test @DisplayName("U-03 | EQ-12 | Retorna 400 cuando no se envía el parámetro electionId")
    void should_return400_when_paramIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/results"))
                .andExpect(status().isBadRequest());
    }

    // S-01 | EQ-20 | Votos por candidato llegan sin alteración
    @Test @DisplayName("S-01 | EQ-20 | Los votos por candidato llegan sin alteración")
    void should_preserveVotesInResponse_when_returningResults() throws Exception {
        ResultsResponse response = ResultsResponse.builder()
                .electionId(1L).totalVotes(5200000)
                .candidates(List.of(new CandidateResult("Juan Pérez", 5200000)))
                .build();

        when(service.getResults(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/results").param("electionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].name").value("Juan Pérez"))
                .andExpect(jsonPath("$.candidates[0].votes").value(5200000));
    }
}
    