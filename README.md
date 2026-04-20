# results-service

## 1. Descripción

El Results Service es un microservicio de solo lectura encargado de
exponer resultados agregados de elecciones.

Forma parte del lado de consulta bajo el enfoque CQRS e implementa una
capa de cache con Redis, incluyendo mecanismos de resiliencia para
tolerar fallos del servicio de cache.

------------------------------------------------------------------------

## 2. Tecnologías

-   Java 21
-   Spring Boot 3.x
-   Spring Web
-   Spring Data JPA
-   PostgreSQL
-   Redis
-   Resilience4j (Circuit Breaker)
-   Flyway
-   Springdoc OpenAPI (Swagger)
-   Maven

------------------------------------------------------------------------

## 3. Arquitectura

Arquitectura por capas:

-   Controller: Exposición de endpoints REST
-   Service: Lógica de negocio y orquestación
-   Repository: Acceso a datos
-   Cache Adapter: Integración con Redis
-   Circuit Breaker: Manejo de fallos en cache
-   Mapper: Transformación de entidades a DTOs

------------------------------------------------------------------------

## 4. Estrategia de Cache

Se implementa el patrón cache-aside con resiliencia:

1.  Se consulta Redis
2.  Si no existe o falla, se consulta la base de datos
3.  Se almacena el resultado en cache
4.  En caso de fallo de Redis, el sistema continúa operando con DB

------------------------------------------------------------------------

## 5. Resiliencia (Circuit Breaker)

Se implementa Circuit Breaker con Resilience4j:

-   Detecta fallos en Redis
-   Evita llamadas innecesarias a servicios caídos
-   Permite fallback automático hacia la base de datos
-   Mejora la latencia en escenarios de fallo

Estados:

-   CLOSED → operación normal
-   OPEN → Redis deshabilitado temporalmente
-   HALF-OPEN → prueba de recuperación

------------------------------------------------------------------------

## 6. Versionamiento API

/api/v1/\*

------------------------------------------------------------------------

## 7. Variables de entorno

DB_URL=jdbc:postgresql://localhost:5432/results_db\
DB_USER=results_user\
DB_PASSWORD=123456

REDIS_HOST=localhost\
REDIS_PORT=6379

PORT=8083

------------------------------------------------------------------------

## 8. Base de datos

CREATE DATABASE results_db;\
CREATE USER results_user WITH PASSWORD '123456';\
GRANT ALL PRIVILEGES ON DATABASE results_db TO results_user;

`\c r`{=tex}esults_db\
GRANT ALL ON SCHEMA public TO results_user;

------------------------------------------------------------------------

## 9. Migraciones (Flyway)

Ubicación:

src/main/resources/db/migration

### V1\_\_init.sql

CREATE TABLE result ( id SERIAL PRIMARY KEY, election_id BIGINT,
candidate_name VARCHAR(255), votes INT );

### V2\_\_seed.sql

INSERT INTO result (election_id, candidate_name, votes) VALUES (1,
'Candidato A', 600), (1, 'Candidato B', 400);

------------------------------------------------------------------------

## 10. Redis

sudo systemctl start redis-server\
redis-cli ping

Respuesta esperada: PONG

------------------------------------------------------------------------

## 11. Ejecución

export \$(grep -v '\^#' .env \| xargs)\
mvn spring-boot:run

------------------------------------------------------------------------

## 12. Swagger

http://localhost:8083/swagger-ui.html

------------------------------------------------------------------------

## 13. Endpoint

GET /api/v1/results?electionId=1

------------------------------------------------------------------------

## 14. Respuesta

{ "electionId": 1, "totalVotes": 1000, "candidates": \[ { "name":
"Candidato A", "votes": 600 }, { "name": "Candidato B", "votes": 400 }
\] }

------------------------------------------------------------------------

## 15. Observabilidad

Logging estructurado:

-   CACHE HIT
-   CACHE MISS
-   CACHE STORE
-   CACHE ERROR
-   CACHE FALLBACK
-   Circuit Breaker events (OPEN, CLOSED, HALF-OPEN)

------------------------------------------------------------------------

## 16. Estado

Microservicio funcional, resiliente y listo para integración:

-   API REST operativa
-   PostgreSQL integrado
-   Redis con tolerancia a fallos
-   Circuit Breaker activo
-   Documentación Swagger
