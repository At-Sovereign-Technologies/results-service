# results-service

## 1. Descripción

El Results Service es un microservicio de solo lectura encargado de exponer resultados agregados de elecciones. Forma parte del sistema ciudadano bajo un enfoque CQRS (lado de consulta).

Incluye cache con Redis para optimizar consultas frecuentes.

---

## 2. Tecnologías

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway
- Springdoc OpenAPI (Swagger)
- Maven

---

## 3. Arquitectura

Capas del servicio:

- Controller → expone endpoints REST
- Service → lógica de negocio y agregación
- Repository → acceso a datos
- Cache Adapter → Redis (cache-aside)
- DTOs → respuestas estructuradas

### Estrategia de cache

1. Se consulta Redis
2. Si no existe → consulta DB
3. Se guarda en cache

---

## 4. Versionamiento API

```
/api/v1/results
```

---

## 5. Variables de entorno (.env)

```
DB_URL=jdbc:postgresql://localhost:5432/results_db
DB_USER=results_user
DB_PASSWORD=123456

REDIS_HOST=localhost
REDIS_PORT=6379

PORT=8083
```

---

## 6. Base de datos

### Crear DB y usuario

```sql
CREATE DATABASE results_db;

CREATE USER results_user WITH PASSWORD '123456';

GRANT ALL PRIVILEGES ON DATABASE results_db TO results_user;

\c results_db
GRANT ALL ON SCHEMA public TO results_user;
```

---

## 7. Migraciones (Flyway)

Ubicación:

```
src/main/resources/db/migration
```

### V1__init.sql

```sql
CREATE TABLE result (
    id SERIAL PRIMARY KEY,
    election_id BIGINT,
    candidate_name VARCHAR(255),
    votes INT
);
```

### V2__seed.sql

```sql
INSERT INTO result (election_id, candidate_name, votes) VALUES
(1, 'Candidato A', 600),
(1, 'Candidato B', 400);
```

---

## 8. Ejecución

```bash
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

---

## 9. Swagger

```
http://localhost:8083/swagger-ui.html
```

---

## 10. Endpoints

### Obtener resultados por elección

```
GET /api/v1/results?electionId=1
```

---

## 11. Respuesta

```json
{
  "electionId": 1,
  "totalVotes": 1000,
  "candidates": [
    { "name": "Candidato A", "votes": 600 },
    { "name": "Candidato B", "votes": 400 }
  ]
}
```

---

## 12. Manejo de errores

### 404

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Results not found"
}
```

---

## 13. Cache (Redis)

Claves:

```
results:{electionId}
```

---

## 14. Estado

Microservicio funcional con:

- Migraciones automatizadas
- Cache en Redis
- Endpoints listos
- Integración lista para API Gateway