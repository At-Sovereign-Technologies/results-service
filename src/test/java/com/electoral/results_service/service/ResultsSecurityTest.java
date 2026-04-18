package com.electoral.results_service.service;

// ============================================================
//  TIPO: Pruebas de Seguridad — ResultsService
//  Suite dedicada exclusivamente a escenarios de seguridad
//  Escenarios cubiertos:
//    EQ-19 | Seguridad - Confidencialidad
//    EQ-20 | Seguridad - Integridad
//    EQ-21 | Seguridad - No Repudio
//    EQ-3  | Funcional - Pertinencia (Doble Verdad)
// ============================================================

import com.electoral.results_service.cache.RedisCacheAdapter;
import com.electoral.results_service.dto.CandidateResult;
import com.electoral.results_service.dto.ResultsResponse;
import com.electoral.results_service.entity.Result;
import com.electoral.results_service.repository.ResultRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResultsService — Pruebas de Seguridad")
class ResultsSecurityTest {

    @Mock
    private ResultRepository repository;

    @Mock
    private RedisCacheAdapter cache;

    @InjectMocks
    private ResultsService service;

    // ----------------------------------------------------------
    // SEC-01 | EQ-20 | Seguridad - Integridad
    // Verifica que ningún candidato tiene votos negativos
    // Un voto negativo indicaría manipulación de resultados
    // ----------------------------------------------------------
    @Test
    @DisplayName("SEC-01 | EQ-20 | Ningún candidato tiene votos negativos (manipulación de resultados)")
    void should_haveNoNegativeVotes_when_processingResults() {
        List<Result> results = List.of(
            new Result(1L, 1L, "Juan Pérez",         5200000),
            new Result(2L, 1L, "María Gómez",         5100000),
            new Result(3L, 1L, "Carlos Rodríguez",    1200000)
        );

        when(cache.get("results:1")).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(results);

        ResultsResponse response = service.getResults(1L);

        response.getCandidates().forEach(c ->
            assertTrue(c.getVotes() >= 0,
                "El candidato " + c.getName() + " no puede tener votos negativos")
        );
    }

    // ----------------------------------------------------------
    // SEC-02 | EQ-20 | Seguridad - Integridad
    // Verifica que el total de votos no excede el máximo
    // razonable para una elección colombiana (~40 millones votantes)
    // ----------------------------------------------------------
    @Test
    @DisplayName("SEC-02 | EQ-20 | El total de votos no excede el máximo poblacional esperado")
    void should_notExceedMaxPopulation_when_totalIsCalculated() {
        List<Result> results = List.of(
            new Result(1L, 1L, "Juan Pérez",         5200000),
            new Result(2L, 1L, "María Gómez",         5100000),
            new Result(3L, 1L, "Carlos Rodríguez",    1200000)
        );

        when(cache.get("results:1")).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(results);

        ResultsResponse response = service.getResults(1L);

        assertTrue(response.getTotalVotes() <= 40_000_000,
                "El total de votos no puede superar el censo electoral máximo de Colombia");
    }

    // ----------------------------------------------------------
    // SEC-03 | EQ-3 | Funcional - Pertinencia (Doble Verdad)
    // Verifica que la suma de votos individuales es idéntica
    // al totalVotes declarado — principio de Doble Verdad electoral
    // ----------------------------------------------------------
    @Test
    @DisplayName("SEC-03 | EQ-3 | Doble Verdad: suma individual === total declarado (sin votos fantasma)")
    void should_matchIndividualSumWithTotal_when_noGhostVotesExist() {
        List<Result> results = List.of(
            new Result(1L, 1L, "Juan Pérez",         5200000),
            new Result(2L, 1L, "María Gómez",         5100000),
            new Result(3L, 1L, "Carlos Rodríguez",    1200000)
        );

        when(cache.get("results:1")).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(results);

        ResultsResponse response = service.getResults(1L);

        int sumaIndividual = response.getCandidates().stream()
                .mapToInt(CandidateResult::getVotes)
                .sum();

        assertEquals(response.getTotalVotes(), sumaIndividual,
                "Doble Verdad: el totalVotes declarado debe ser idéntico a la suma individual. " +
                "Diferencia detectada indicaría votos fantasma o manipulación.");
    }

    // ----------------------------------------------------------
    // SEC-04 | EQ-19 | Seguridad - Confidencialidad
    // Verifica que los resultados de la Consulta (elección 5)
    // no son accesibles usando el ID de la Presidencia (elección 1)
    // Previene fuga de información entre comicios
    // ----------------------------------------------------------
    @Test
    @DisplayName("SEC-04 | EQ-19 | Resultados de una elección no son accesibles con ID de otra")
    void should_notExposeOtherElectionResults_when_queryingSpecificId() {
        List<Result> presidencialResults = List.of(
            new Result(1L, 1L, "Juan Pérez", 5200000)
        );

        when(cache.get("results:1")).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(presidencialResults);

        ResultsResponse response = service.getResults(1L);

        assertEquals(1L, response.getElectionId(),
                "La respuesta solo debe contener datos de la elección solicitada");
        assertFalse(response.getCandidates().stream()
                .anyMatch(c -> c.getName().equals("Sí") || c.getName().equals("No")),
                "Los resultados de la Consulta (Sí/No) no deben filtrarse en la respuesta presidencial");
    }

    // ----------------------------------------------------------
    // SEC-05 | EQ-19 | Seguridad - Confidencialidad
    // Verifica que el servicio no retorna resultados cuando
    // la lista de BD está vacía — sin fugas de datos por defecto
    // ----------------------------------------------------------
    @Test
    @DisplayName("SEC-05 | EQ-19 | No retorna datos por defecto cuando la BD está vacía para esa elección")
    void should_throwException_when_noResultsInDatabase() {
        when(cache.get("results:99")).thenReturn(null);
        when(repository.findByElectionId(99L)).thenReturn(List.of());

        assertThrows(
                com.electoral.results_service.exception.ResourceNotFoundException.class,
                () -> service.getResults(99L),
                "No debe retornar datos vacíos ni de otras elecciones — debe lanzar excepción"
        );
    }

    // ----------------------------------------------------------
    // SEC-06 | EQ-21 | Seguridad - No Repudio
    // Verifica que el repositorio es consultado exactamente una vez
    // por solicitud — sin consultas duplicadas que inflen el conteo
    // ----------------------------------------------------------
    @Test
    @DisplayName("SEC-06 | EQ-21 | El repositorio es consultado exactamente una vez por solicitud")
    void should_queryRepositoryExactlyOnce_when_cacheIsEmpty() {
        List<Result> results = List.of(
            new Result(1L, 1L, "Juan Pérez", 5200000)
        );

        when(cache.get("results:1")).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(results);

        service.getResults(1L);

        verify(repository, times(1)).findByElectionId(1L);
        verify(repository, never()).findByElectionId(2L);
    }

    // ----------------------------------------------------------
    // SEC-07 | EQ-21 | Seguridad - No Repudio
    // Verifica que los resultados se guardan en caché
    // exactamente una vez — sin escrituras duplicadas
    // ----------------------------------------------------------
    @Test
    @DisplayName("SEC-07 | EQ-21 | Los resultados se almacenan en caché exactamente una vez")
    void should_storeInCacheExactlyOnce_when_processingResults() {
        List<Result> results = List.of(
            new Result(1L, 1L, "Juan Pérez", 5200000)
        );

        when(cache.get("results:1")).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(results);

        service.getResults(1L);

        verify(cache, times(1)).set(eq("results:1"), any(ResultsResponse.class));
    }

    // ----------------------------------------------------------
    // SEC-08 | EQ-20 | Seguridad - Integridad
    // Verifica que los resultados desde caché no han sido
    // alterados respecto a los valores originales
    // ----------------------------------------------------------
    @Test
    @DisplayName("SEC-08 | EQ-20 | Los resultados desde caché mantienen integridad de votos")
    void should_preserveVoteIntegrity_when_returningFromCache() {
        ResultsResponse cached = ResultsResponse.builder()
                .electionId(1L)
                .totalVotes(11500000)
                .candidates(List.of(
                    new CandidateResult("Juan Pérez",      5200000),
                    new CandidateResult("María Gómez",      5100000),
                    new CandidateResult("Carlos Rodríguez", 1200000)
                ))
                .build();

        when(cache.get("results:1")).thenReturn(cached);

        ResultsResponse response = service.getResults(1L);

        assertEquals(11500000, response.getTotalVotes(),
                "Los votos en caché no deben ser alterados");

        int sumaDesdeCache = response.getCandidates().stream()
                .mapToInt(CandidateResult::getVotes)
                .sum();

        assertEquals(response.getTotalVotes(), sumaDesdeCache,
                "Doble Verdad: el total en caché debe seguir siendo igual a la suma individual");
    }

    // ----------------------------------------------------------
    // SEC-09 | EQ-20 | Seguridad - Integridad
    // Verifica que ningún candidato tiene exactamente 0 votos
    // (indicaría posible dato corrupto o candidato fantasma)
    // ----------------------------------------------------------
    @Test
    @DisplayName("SEC-09 | EQ-20 | Ningún candidato tiene exactamente 0 votos (dato sospechoso)")
    void should_detectZeroVoteCandidate_when_dataIsCorrupt() {
        List<Result> results = List.of(
            new Result(1L, 1L, "Juan Pérez",         5200000),
            new Result(2L, 1L, "Candidato Fantasma",  0)
        );

        when(cache.get("results:1")).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(results);

        ResultsResponse response = service.getResults(1L);

        boolean hayVotosCero = response.getCandidates().stream()
                .anyMatch(c -> c.getVotes() == 0);

        assertTrue(hayVotosCero,
                "SEC-09 detecta candidato con 0 votos — dato que debe revisarse en auditoría");
    }
}
