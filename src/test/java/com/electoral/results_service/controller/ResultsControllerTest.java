package com.electoral.results_service.controller;

// ============================================================
//  TIPO: Pruebas de Integración
// ============================================================

import com.electoral.results_service.dto.CandidateResult;
import com.electoral.results_service.dto.ResultsResponse;
import com.electoral.results_service.exception.ResourceNotFoundException;
import com.electoral.results_service.service.ResultsService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResultsController.class)
@DisplayName("ResultsController — Pruebas de Integración y Validación")
class ResultsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResultsService service;

    // ----------------------------------------------------------
    // RC-01 | EQ-2 | Funcional - Corrección
    // Verifica respuesta 200 con todos los campos correctos
    // ----------------------------------------------------------
    @Test
    @DisplayName("RC-01 | EQ-2 | Retorna 200 con resultados correctos para electionId válido")
    void should_return200_when_electionIdIsValid() throws Exception {
        ResultsResponse response = ResultsResponse.builder()
                .electionId(1L)
                .totalVotes(11500000)
                .candidates(List.of(
                    new CandidateResult("Juan Pérez",       5200000),
                    new CandidateResult("María Gómez",       5100000),
                    new CandidateResult("Carlos Rodríguez",  1200000)
                ))
                .build();

        when(service.getResults(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/results")
                .param("electionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.electionId").value(1))
                .andExpect(jsonPath("$.totalVotes").value(11500000))
                .andExpect(jsonPath("$.candidates.length()").value(3));
    }

    // ----------------------------------------------------------
    // RC-02 | EQ-3 | Funcional - Pertinencia (Doble Verdad)
    // Verifica que el total de votos en la respuesta coincide
    // con la suma de votos por candidato
    // ----------------------------------------------------------
    @Test
    @DisplayName("RC-02 | EQ-3 | Doble Verdad: totalVotes coincide con suma de candidatos")
    void should_matchTotalWithCandidateSum_when_returningResults() throws Exception {
        ResultsResponse response = ResultsResponse.builder()
                .electionId(1L)
                .totalVotes(11500000)
                .candidates(List.of(
                    new CandidateResult("Juan Pérez",      5200000),
                    new CandidateResult("María Gómez",      5100000),
                    new CandidateResult("Carlos Rodríguez", 1200000)
                ))
                .build();

        when(service.getResults(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/results")
                .param("electionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(
                    5200000 + 5100000 + 1200000));
    }

    // ----------------------------------------------------------
    // RC-03 | EQ-19 | Seguridad - Confidencialidad
    // Verifica que una elección inexistente retorna 404
    // y no expone información de otras elecciones
    // ----------------------------------------------------------
    @Test
    @DisplayName("RC-03 | EQ-19 | Retorna 404 para elección inexistente sin exponer otros datos")
    void should_return404_when_electionNotFound() throws Exception {
        when(service.getResults(999L))
                .thenThrow(new ResourceNotFoundException("Results not found"));

        mockMvc.perform(get("/api/v1/results")
                .param("electionId", "999"))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------
    // RC-04 | EQ-12 | Usabilidad - Protección contra Errores
    // Verifica rechazo cuando el electionId contiene letras
    // ----------------------------------------------------------
    @Test
    @DisplayName("RC-04 | EQ-12 | Retorna 400 cuando electionId contiene letras")
    void should_return400_when_electionIdContainsLetters() throws Exception {
        mockMvc.perform(get("/api/v1/results")
                .param("electionId", "ABC"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------
    // RC-05 | EQ-12 | Usabilidad - Protección contra Errores
    // Verifica rechazo cuando no se envía el parámetro electionId
    // ----------------------------------------------------------
    @Test
    @DisplayName("RC-05 | EQ-12 | Retorna 400 cuando el parámetro electionId no se envía")
    void should_return400_when_electionIdIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/results"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------
    // RC-06 | EQ-12 | Usabilidad - Protección contra Errores
    // Verifica rechazo cuando el electionId tiene caracteres especiales
    // ----------------------------------------------------------
    @Test
    @DisplayName("RC-06 | EQ-12 | Retorna 400 cuando electionId tiene caracteres especiales")
    void should_return400_when_electionIdHasSpecialChars() throws Exception {
        mockMvc.perform(get("/api/v1/results")
                .param("electionId", "1-2"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------
    // RC-07 | EQ-20 | Seguridad - Integridad
    // Verifica que la respuesta incluye el electionId correcto
    // sin contaminación cruzada entre elecciones
    // ----------------------------------------------------------
    @Test
    @DisplayName("RC-07 | EQ-20 | El electionId en respuesta coincide con el solicitado")
    void should_returnCorrectElectionId_when_queried() throws Exception {
        ResultsResponse response = ResultsResponse.builder()
                .electionId(3L)
                .totalVotes(1880000)
                .candidates(List.of(
                    new CandidateResult("Luis Martínez", 800000),
                    new CandidateResult("Ana Torres",    780000),
                    new CandidateResult("Diego Ramírez", 300000)
                ))
                .build();

        when(service.getResults(3L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/results")
                .param("electionId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.electionId").value(3));
    }

    // ----------------------------------------------------------
    // RC-08 | EQ-20 | Seguridad - Integridad
    // Verifica que los votos de cada candidato llegan
    // sin alteración en la respuesta HTTP
    // ----------------------------------------------------------
    @Test
    @DisplayName("RC-08 | EQ-20 | Los votos por candidato llegan sin alteración en la respuesta")
    void should_preserveVotesInResponse_when_returningResults() throws Exception {
        ResultsResponse response = ResultsResponse.builder()
                .electionId(1L)
                .totalVotes(11500000)
                .candidates(List.of(
                    new CandidateResult("Juan Pérez", 5200000)
                ))
                .build();

        when(service.getResults(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/results")
                .param("electionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].name").value("Juan Pérez"))
                .andExpect(jsonPath("$.candidates[0].votes").value(5200000));
    }
}
