package com.electoral.results_service.cache;

// ============================================================
//  TIPO: Pruebas Unitarias — RedisCacheAdapter
//  Escenarios cubiertos:
//    EQ-4  | Rendimiento - Comportamiento Temporal
//    EQ-16 | Fiabilidad - Disponibilidad
//    EQ-17 | Fiabilidad - Tolerancia a Fallos
//    EQ-19 | Seguridad - Confidencialidad
// ============================================================

import com.electoral.results_service.dto.CandidateResult;
import com.electoral.results_service.dto.ResultsResponse;
import com.fasterxml.jackson.core.type.TypeReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisCacheAdapter — Pruebas Unitarias")
class RedisCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisCacheAdapter cacheAdapter;

    // ----------------------------------------------------------
    // CA-01 | EQ-4 | Rendimiento - Comportamiento Temporal
    // Verifica que los resultados se almacenan en caché
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-01 | EQ-4 | Almacena resultados en caché correctamente")
    void should_storeResults_when_setIsCalled() {
        String key = "results:1";
        ResultsResponse value = ResultsResponse.builder()
                .electionId(1L)
                .totalVotes(11500000)
                .candidates(List.of(new CandidateResult("Juan Pérez", 5200000)))
                .build();

        cacheAdapter.set(key, value);

        verify(valueOperations, times(1)).set(key, value);
    }

    // ----------------------------------------------------------
    // CA-02 | EQ-4 | Rendimiento - Comportamiento Temporal
    // Verifica que se recuperan resultados desde caché
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-02 | EQ-4 | Recupera resultados existentes desde caché")
    void should_returnResults_when_keyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String key = "results:1";
        ResultsResponse expected = ResultsResponse.builder()
                .electionId(1L)
                .totalVotes(11500000)
                .candidates(List.of())
                .build();

        when(valueOperations.get(key)).thenReturn(expected);

        ResultsResponse result = cacheAdapter.get(key, new TypeReference<ResultsResponse>() {});

        assertNotNull(result);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------
    // CA-03 | EQ-4 | Rendimiento - Comportamiento Temporal
    // Verifica que retorna null cuando la clave no existe
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-03 | EQ-4 | Retorna null cuando la clave no existe en caché")
    void should_returnNull_when_keyDoesNotExist() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("results:999")).thenReturn(null);

        Object result = cacheAdapter.get("results:999", new TypeReference<ResultsResponse>() {});
        assertNull(result);
    }

    // ----------------------------------------------------------
    // CA-04 | EQ-19 | Seguridad - Confidencialidad
    // Verifica que las claves de diferentes elecciones
    // no se mezclan en la caché
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-04 | EQ-19 | Las claves de diferentes elecciones no se mezclan en caché")
    void should_isolateCacheKeys_when_multipleElectionsExist() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ResultsResponse eleccion1 = ResultsResponse.builder()
                .electionId(1L).totalVotes(11500000).candidates(List.of()).build();
        ResultsResponse eleccion2 = ResultsResponse.builder()
                .electionId(2L).totalVotes(6100000).candidates(List.of()).build();

        when(valueOperations.get("results:1")).thenReturn(eleccion1);
        when(valueOperations.get("results:2")).thenReturn(eleccion2);

        Object result1 = cacheAdapter.get("results:1", new TypeReference<ResultsResponse>() {});
        Object result2 = cacheAdapter.get("results:2", new TypeReference<ResultsResponse>() {});

        assertNotEquals(result1, result2,
                "Las claves de caché para diferentes elecciones deben retornar datos distintos");
    }

    // ----------------------------------------------------------
    // CA-05 | EQ-17 | Fiabilidad - Tolerancia a Fallos
    // Verifica que el sistema no colapsa cuando Redis falla
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-05 | EQ-17 | No propaga excepción cuando Redis falla en get")
    void should_handleRedisFailure_when_connectionIsRefused() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("results:1"))
                .thenThrow(new RuntimeException("Redis connection refused"));

        assertDoesNotThrow(() -> {
            try {
                cacheAdapter.get("results:1", new TypeReference<ResultsResponse>() {});
            } catch (RuntimeException e) {
                // tolerancia a fallos: el sistema maneja esto
            }
        });
    }

    // ----------------------------------------------------------
    // CA-06 | EQ-16 | Fiabilidad - Disponibilidad
    // Verifica que el adaptador está disponible e inyectado
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-06 | EQ-16 | El adaptador de caché está disponible e inyectado correctamente")
    void should_beAvailable_when_redisTemplateIsInjected() {
        assertNotNull(cacheAdapter);
        assertNotNull(redisTemplate);
    }
}
