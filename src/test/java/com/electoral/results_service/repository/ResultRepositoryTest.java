package com.electoral.results_service.repository;

// ============================================================
//  TIPO: Pruebas de Integración — ResultRepository
//  Escenarios cubiertos:
//    EQ-2  | Funcional - Corrección
//    EQ-20 | Seguridad - Integridad
//    EQ-3  | Funcional - Pertinencia (Doble Verdad)
// ============================================================

import com.electoral.results_service.entity.Result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("ResultRepository — Pruebas de Integración con BD")
class ResultRepositoryTest {

    @Autowired
    private ResultRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Presidencial
        repository.saveAll(List.of(
            new Result(null, 1L, "Juan Pérez",         5200000),
            new Result(null, 1L, "María Gómez",         5100000),
            new Result(null, 1L, "Carlos Rodríguez",    1200000),
            // Senado
            new Result(null, 2L, "Partido A",           2200000),
            new Result(null, 2L, "Partido B",           2100000),
            new Result(null, 2L, "Partido C",           1800000)
        ));
    }

    // ----------------------------------------------------------
    // RR-01 | EQ-2 | Funcional - Corrección
    // Verifica que findByElectionId retorna solo los registros
    // de la elección solicitada
    // ----------------------------------------------------------
    @Test
    @DisplayName("RR-01 | EQ-2 | findByElectionId retorna solo candidatos de esa elección")
    void should_returnOnlyCandidatesForElection_when_queryingById() {
        List<Result> results = repository.findByElectionId(1L);

        assertEquals(3, results.size(),
                "La presidencial debe tener exactamente 3 candidatos");
        assertTrue(results.stream()
                .allMatch(r -> r.getElectionId().equals(1L)),
                "Todos los resultados deben pertenecer a la elección 1");
    }

    // ----------------------------------------------------------
    // RR-02 | EQ-20 | Seguridad - Integridad
    // Verifica que los votos persisten sin alteración en BD
    // ----------------------------------------------------------
    @Test
    @DisplayName("RR-02 | EQ-20 | Los votos se persisten sin alteración en la base de datos")
    void should_persistVotesIntact_when_savedToDatabase() {
        List<Result> results = repository.findByElectionId(1L);

        Result juanPerez = results.stream()
                .filter(r -> r.getCandidateName().equals("Juan Pérez"))
                .findFirst()
                .orElseThrow();

        assertEquals(5200000, juanPerez.getVotes(),
                "Los votos de Juan Pérez deben persistir exactamente como fueron guardados");
    }

    // ----------------------------------------------------------
    // RR-03 | EQ-20 | Seguridad - Integridad
    // Verifica que los resultados del Senado no aparecen
    // al consultar la Presidencia
    // ----------------------------------------------------------
    @Test
    @DisplayName("RR-03 | EQ-20 | Resultados de Senado no aparecen al consultar Presidencia")
    void should_isolateResultsByElection_when_multipleElectionsExist() {
        List<Result> presidencial = repository.findByElectionId(1L);

        boolean contieneSenado = presidencial.stream()
                .anyMatch(r -> r.getCandidateName().equals("Partido A")
                        || r.getCandidateName().equals("Partido B"));

        assertFalse(contieneSenado,
                "Los resultados del Senado no deben filtrarse en la consulta presidencial");
    }

    // ----------------------------------------------------------
    // RR-04 | EQ-3 | Funcional - Pertinencia (Doble Verdad)
    // Verifica que la suma de votos desde BD coincide
    // con el total esperado
    // ----------------------------------------------------------
    @Test
    @DisplayName("RR-04 | EQ-3 | Doble Verdad: suma de votos en BD coincide con total esperado")
    void should_matchExpectedTotal_when_summingVotesFromDB() {
        List<Result> results = repository.findByElectionId(1L);

        int totalCalculado = results.stream()
                .mapToInt(Result::getVotes)
                .sum();

        assertEquals(11500000, totalCalculado,
                "La suma de votos en BD debe ser 5200000 + 5100000 + 1200000 = 11500000");
    }

    // ----------------------------------------------------------
    // RR-05 | EQ-2 | Funcional - Corrección
    // Verifica que retorna lista vacía para elección inexistente
    // ----------------------------------------------------------
    @Test
    @DisplayName("RR-05 | EQ-2 | Retorna lista vacía para elección no registrada")
    void should_returnEmptyList_when_electionDoesNotExist() {
        List<Result> results = repository.findByElectionId(999L);

        assertTrue(results.isEmpty(),
                "No debe retornar resultados para una elección no registrada");
    }
}
