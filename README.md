# Tienda API

API REST de gestión de clientes, productos y pedidos, construida con Spring Boot 4
sobre una arquitectura en capas, con validación, manejo estructurado de errores,
tests de integración y documentación OpenAPI.

<!-- Insertá acá los badges cuando el CI esté verde:
[![CI](https://github.com/USUARIO/tienda-api/actions/workflows/ci.yml/badge.svg)](https://github.com/USUARIO/tienda-api/actions/workflows/ci.yml)
-->

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-green)

---

## Qué resuelve

<!-- Tres o cuatro líneas: qué hace la API y qué problema modela.
     Concreto, sin marketing. Ejemplo:

     Gestiona el catálogo de productos y el ciclo de vida de un pedido: alta de
     clientes, validación de stock al confirmar, cálculo de totales y transiciones
     de estado. -->

## Decisiones de diseño

<!-- La sección que un entrevistador lee. Una decisión por bullet, con el porqué
     y la alternativa descartada. Ejemplos del nivel esperado:

- **Las entidades JPA no salen del `service`.** Los controllers reciben y devuelven
  `record`s. Exponer la entidad acopla el contrato HTTP al esquema de base y arrastra
  relaciones perezosas a la serialización.
- **Inyección por constructor, sin `@Autowired`.** Permite campos `final`, hace
  explícitas las dependencias y deja construir la clase en un test sin contexto.
- **Errores como `ProblemDetail` (RFC 7807).** Un formato de error estándar en toda
  la API, en vez de un mapa improvisado por endpoint.
- **`@Transactional` en el service.** El controller no conoce transacciones; el
  repository no decide límites. -->

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.x |
| Persistencia | Spring Data JPA · H2 (dev) · PostgreSQL |
| Validación | Bean Validation |
| Documentación | springdoc-openapi |
| Testing | JUnit 5 · AssertJ · Mockito · Testcontainers |
| Build | Maven |
| CI | GitHub Actions |

## Arquitectura

```
controller/   recibe HTTP, valida entrada, devuelve DTOs
    ↓
service/      reglas de negocio y transacciones
    ↓
repository/   acceso a datos
    ↓
domain/       entidades JPA
```

<!-- Si agregás un diagrama, va acá. -->

## Cómo ejecutarlo

Requisitos: JDK 21. Docker solo si querés usar PostgreSQL.

```bash
git clone https://github.com/USUARIO/tienda-api.git
cd tienda-api
./mvnw spring-boot:run
```

Arranca en `http://localhost:8080` con H2 en memoria.

Con PostgreSQL:

```bash
cp .env.example .env
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Documentación de la API

Con la aplicación corriendo:

- Swagger UI — http://localhost:8080/swagger-ui.html
- OpenAPI — http://localhost:8080/v3/api-docs
- Health — http://localhost:8080/actuator/health

<!-- Cuando tengas los endpoints, poné acá una tabla:

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/clientes` | Lista paginada |
| `POST` | `/api/v1/clientes` | Alta |
-->

## Tests

```bash
./mvnw test              # unitarios
./mvnw verify            # unitarios + integración (requiere Docker)
```

<!-- Cuando midas la cobertura, mencioná el número y qué cubre. -->

## Notas de aprendizaje

Las decisiones y el funcionamiento interno del framework están documentados en
[`docs/`](docs/):

<!-- Se va llenando a medida que avanzan los milestones:
- [Anatomía de una petición HTTP](docs/01-anatomia-de-una-peticion.md)
- [El contenedor IoC y el ciclo de vida de un bean](docs/02-contenedor-ioc.md)
-->

## Estado

<!-- Actualizá al cerrar cada milestone:
- [x] M1 — Fundaciones
- [ ] M2 — Capa Controller
-->

---

## Licencia

MIT
