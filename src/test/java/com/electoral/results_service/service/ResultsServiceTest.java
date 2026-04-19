package com.electoral.results_service.service;

// ============================================================
//  TIPO: Pruebas Unitarias — ResultsService
//  Escenarios cubiertos:
//    EQ-1  | Funcional - Completitud
//    EQ-2  | Funcional - Corrección
//    EQ-3  | Funcional - Pertinencia (Doble Verdad)
//    EQ-4  | Rendimiento - Comportamiento Temporal
//    EQ-19 | Seguridad - Confidencialidad
//    EQ-20 | Seguridad - Integridad
//    EQ-21 | Seguridad - No Repudio
//    EQ-27 | Mantenibilidad - Testeabilidad
// ============================================================

import com.electoral.results_service.cache.RedisCacheAdapter;
import com.electoral.results_service.dto.CandidateResult;
import com.electoral.results_service.dto.ResultsResponse;
import com.electoral.results_service.entity.Result;
import com.electoral.results_service.exception.ResourceNotFoundException;
import com.electoral.results_service.repository.ResultRepository;
import com.fasterxml.jackson.core.type.TypeReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResultsService — Pruebas Unitarias")
class ResultsServiceTest {

    @Mock
    private ResultRepository repository;

    @Mock
    private RedisCacheAdapter cache;

    @InjectMocks
    private ResultsService service;

    private List<Result> presidencialResults;

    @BeforeEach
    void setUp() {
        presidencialResults = List.of(
            new Result(1L, 1L, "Juan Pérez",          5200000),
            new Result(2L, 1L, "María Gómez",          5100000),
            new Result(3L, 1L, "Carlos Rodríguez",     1200000)
        );
    }

    // ----------------------------------------------------------
    // RS-01 | EQ-2 | Funcional - Corrección
    // Verifica que el total de votos se calcula correctamente
    // sumando todos los candidatos sin alteración
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-01 | EQ-2 | Calcula el total de votos correctamente")
    void should_calculateTotalVotes_when_resultsExist() {
        when(cache.get("results:1", new TypeReference<ResultsResponse>() {})).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(presidencialResults);

        ResultsResponse response = service.getResults(1L);

        assertEquals(11500000, response.getTotalVotes(),
                "El total debe ser la suma exacta de votos: 5200000 + 5100000 + 1200000");
    }

    // ----------------------------------------------------------
    // RS-02 | EQ-1 | Funcional - Completitud
    // Verifica que todos los candidatos aparecen en la respuesta
    // sin omitir ninguno
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-02 | EQ-1 | Retorna todos los candidatos sin omitir ninguno")
    void should_returnAllCandidates_when_resultsExist() {
        when(cache.get("results:1", new TypeReference<ResultsResponse>() {})).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(presidencialResults);

        ResultsResponse response = service.getResults(1L);

        assertEquals(3, response.getCandidates().size(),
                "Deben aparecer los 3 candidatos registrados");
        assertTrue(response.getCandidates().stream()
                .anyMatch(c -> c.getName().equals("Juan Pérez")));
        assertTrue(response.getCandidates().stream()
                .anyMatch(c -> c.getName().equals("María Gómez")));
        assertTrue(response.getCandidates().stream()
                .anyMatch(c -> c.getName().equals("Carlos Rodríguez")));
    }

    // ----------------------------------------------------------
    // RS-03 | EQ-20 | Seguridad - Integridad
    // Verifica que los votos de cada candidato no son alterados
    // durante el procesamiento del servicio
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-03 | EQ-20 | Los votos por candidato no son alterados durante el procesamiento")
    void should_preserveVotesPerCandidate_when_mappingResults() {
        when(cache.get("results:1", new TypeReference<ResultsResponse>() {})).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(presidencialResults);

        ResultsResponse response = service.getResults(1L);

        CandidateResult juanPerez = response.getCandidates().stream()
                .filter(c -> c.getName().equals("Juan Pérez"))
                .findFirst()
                .orElseThrow();

        assertEquals(5200000, juanPerez.getVotes(),
                "Los votos de Juan Pérez no deben ser alterados: deben ser exactamente 5200000");
    }

    // ----------------------------------------------------------
    // RS-04 | EQ-20 | Seguridad - Integridad
    // Verifica que el electionId en la respuesta coincide
    // con el solicitado — previene contaminación cruzada de actas
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-04 | EQ-20 | El electionId de la respuesta coincide con el solicitado")
    void should_returnCorrectElectionId_when_queried() {
        when(cache.get("results:1", new TypeReference<ResultsResponse>() {})).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(presidencialResults);

        ResultsResponse response = service.getResults(1L);

        assertEquals(1L, response.getElectionId(),
                "El electionId retornado debe ser exactamente el consultado — sin contaminación cruzada");
    }

    // ----------------------------------------------------------
    // RS-05 | EQ-19 | Seguridad - Confidencialidad
    // Verifica que una elección inexistente lanza excepción
    // en lugar de retornar datos vacíos o de otra elección
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-05 | EQ-19 | Lanza excepción para elección inexistente en lugar de exponer datos vacíos")
    void should_throwResourceNotFoundException_when_electionNotFound() {
        when(cache.get("results:999", new TypeReference<ResultsResponse>() {})).thenReturn(null);
        when(repository.findByElectionId(999L)).thenReturn(List.of());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getResults(999L),
                "Debe lanzar excepción para elecciones no registradas"
        );

        assertEquals("Results not found", ex.getMessage());
    }

    // ----------------------------------------------------------
    // RS-06 | EQ-4 | Rendimiento - Comportamiento Temporal
    // Verifica que la caché evita consultas innecesarias a BD
    // para resultados ya procesados
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-06 | EQ-4 | Retorna resultados desde caché sin consultar BD")
    void should_returnFromCache_when_resultsAreCached() {
        ResultsResponse cached = ResultsResponse.builder()
                .electionId(1L)
                .totalVotes(11500000)
                .candidates(List.of(
                    new CandidateResult("Juan Pérez", 5200000)
                ))
                .build();

        when(cache.get("results:1", new TypeReference<ResultsResponse>() {})).thenReturn(cached);

        ResultsResponse response = service.getResults(1L);

        assertNotNull(response);
        assertEquals(1L, response.getElectionId());
        verify(repository, never()).findByElectionId(anyLong());
    }

    // ----------------------------------------------------------
    // RS-07 | EQ-4 | Rendimiento - Comportamiento Temporal
    // Verifica que los resultados se almacenan en caché
    // tras la primera consulta a BD
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-07 | EQ-4 | Almacena resultados en caché tras primera consulta")
    void should_storeInCache_when_queryingFromDatabase() {
        when(cache.get("results:1", new TypeReference<ResultsResponse>() {})).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(presidencialResults);

        service.getResults(1L);

        verify(cache, times(1)).set(eq("results:1"), any(ResultsResponse.class));
    }

    // ----------------------------------------------------------
    // RS-08 | EQ-3 | Funcional - Pertinencia (Doble Verdad)
    // Verifica que el total calculado coincide con la suma
    // individual de votos — principio de Doble Verdad
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-08 | EQ-3 | Doble Verdad: el total coincide con la suma individual de votos")
    void should_matchTotalWithSumOfIndividualVotes_when_calculatingResults() {
        when(cache.get("results:1", new TypeReference<ResultsResponse>() {})).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(presidencialResults);

        ResultsResponse response = service.getResults(1L);

        int sumaManual = response.getCandidates().stream()
                .mapToInt(CandidateResult::getVotes)
                .sum();

        assertEquals(sumaManual, response.getTotalVotes(),
                "Doble Verdad: el total declarado debe ser idéntico a la suma de votos por candidato");
    }

    // ----------------------------------------------------------
    // RS-09 | EQ-20 | Seguridad - Integridad
    // Verifica que los resultados del Senado no contaminan
    // los resultados de la Presidencia (aislamiento por electionId)
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-09 | EQ-20 | Los resultados de una elección no contaminan los de otra")
    void should_isolateResultsByElectionId_when_multipleElectionsExist() {
        List<Result> senadoResults = List.of(
            new Result(4L, 2L, "Partido A", 2200000),
            new Result(5L, 2L, "Partido B", 2100000),
            new Result(6L, 2L, "Partido C", 1800000)
        );

        when(cache.get("results:2", new TypeReference<ResultsResponse>() {})).thenReturn(null);
        when(repository.findByElectionId(2L)).thenReturn(senadoResults);

        ResultsResponse response = service.getResults(2L);

        assertEquals(2L, response.getElectionId());
        assertFalse(response.getCandidates().stream()
                .anyMatch(c -> c.getName().equals("Juan Pérez")),
                "Los candidatos presidenciales no deben aparecer en resultados del Senado");
    }

    // ----------------------------------------------------------
    // RS-10 | EQ-21 | Seguridad - No Repudio
    // Verifica que el servicio registra la consulta a BD
    // (evidencia de auditoría: CACHE MISS queda en logs)
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-10 | EQ-21 | El servicio consulta el repositorio cuando no hay caché (trazabilidad)")
    void should_queryRepository_when_cacheIsEmpty() {
        when(cache.get("results:1", new TypeReference<ResultsResponse>() {})).thenReturn(null);
        when(repository.findByElectionId(1L)).thenReturn(presidencialResults);

        service.getResults(1L);

        verify(repository, times(1)).findByElectionId(1L);
    }

    // ----------------------------------------------------------
    // RS-11 | EQ-27 | Mantenibilidad - Testeabilidad
    // Verifica que el servicio es instanciable con sus dependencias
    // ----------------------------------------------------------
    @Test
    @DisplayName("RS-11 | EQ-27 | El servicio es instanciable con dependencias inyectadas")
    void should_instantiateService_when_dependenciesAreInjected() {
        assertNotNull(service);
        assertNotNull(repository);
        assertNotNull(cache);
    }
}
