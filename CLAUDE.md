# CLAUDE.md

Estándares de ingeniería de este repositorio.

---

## Qué es este proyecto

API REST de una tienda: clientes, productos y pedidos. Construida siguiendo la
arquitectura en capas clásica de Spring Boot — Controller, Service, Repository —
con validación, manejo de errores, tests y documentación.

Es una base de referencia: cada decisión está documentada y cada bean explicado.

---

## Stack

- Java 21
- Spring Boot 4.x
- Maven, siempre vía `./mvnw`
- Spring Web, Spring Data JPA, Bean Validation
- H2 en desarrollo, PostgreSQL vía Docker Compose
- JUnit 5, AssertJ, Mockito, Testcontainers
- springdoc-openapi

**Nunca uses APIs de Spring Boot 2.x.** Traducción obligatoria:

| Obsoleto | Actual |
|---|---|
| `javax.persistence.*` | `jakarta.persistence.*` |
| `javax.validation.*` | `jakarta.validation.*` |
| `@Autowired` en campo o setter | Inyección por constructor, sin anotación |
| `RestTemplate` | `RestClient` |
| `WebSecurityConfigurerAdapter` | `SecurityFilterChain` como `@Bean` |
| `@MockBean` / `@SpyBean` | `@MockitoBean` / `@MockitoSpyBean` |
| Clase con `@Data` para un DTO | `record` |

---

## Arquitectura en capas

```
controller/   → recibe HTTP, valida entrada, devuelve DTOs. Sin lógica de negocio.
service/      → reglas de negocio, transacciones, orquestación. Sin nada de HTTP.
repository/   → acceso a datos. Interfaces de Spring Data.
domain/       → entidades JPA.
dto/          → records de entrada y salida. Nunca se expone una entidad.
exception/    → excepciones de negocio y manejador global.
config/       → clases @Configuration.
```

Reglas que no se negocian:

- **Una entidad JPA nunca aparece en la firma de un método de un controller.**
  Ni como parámetro ni como retorno. Entra y sale un DTO.
- **El controller no tiene lógica de negocio.** Si hay un `if` sobre una regla del
  dominio, va al service.
- **El service no conoce HTTP.** Nada de `ResponseEntity`, `HttpStatus` ni
  `HttpServletRequest` en esa capa.
- **La transacción vive en el service**, nunca en el controller ni en el repository.
- **Inyección por constructor**, campos `final`. Nunca `@Autowired` en un campo:
  esconde las dependencias, impide el `final` y obliga a levantar el contexto
  para testear.

---

## Javadoc — la regla central de este repo

**Toda clase que sea un bean lleva Javadoc explicando por qué es un bean.**
No basta con describir qué hace: hay que documentar la decisión.

Cada clase anotada con `@Component`, `@Service`, `@Repository`, `@Controller`,
`@RestController`, `@Configuration`, o cada método `@Bean`, debe documentar:

1. **Qué estereotipo usa y por qué ese y no otro.**
2. **Su scope** y por qué (singleton por defecto: decir qué implica).
3. **Sus dependencias** y cómo se inyectan.
4. **Su rol en el flujo** de una petición.

Ejemplo del nivel esperado:

```java
/**
 * Reglas de negocio de pedidos: validación de stock, cálculo de total y
 * transiciones de estado.
 *
 * <p>Anotada con {@code @Service} y no con {@code @Component} porque, si bien
 * ambas registran el bean de la misma forma, {@code @Service} declara la
 * intención de la clase. El estereotipo es documentación ejecutable: cualquiera
 * que lea la clase sabe que acá vive lógica de negocio y no acceso a datos.
 *
 * <p>Scope singleton (el de por defecto): existe una sola instancia en el
 * contenedor, compartida por todas las peticiones. Por eso la clase no tiene
 * estado mutable — un campo de instancia acá sería compartido entre hilos.
 *
 * <p>Dependencias inyectadas por constructor, lo que permite declararlas
 * {@code final} y construir la clase en un test sin levantar el contexto.
 */
@Service
public class PedidoService {
```

Además:

- Todo método público de controller y service lleva Javadoc con `@param`,
  `@return` y `@throws`.
- Las reglas de negocio se explican en el Javadoc del método que las implementa.
- Cuando una anotación de Spring tenga un comportamiento no obvio
  (`@Transactional(readOnly = true)`, `@Lazy`, `@Primary`, `@Qualifier`),
  el Javadoc dice **por qué está**.

---

## Git

Trunk-based: `main` protegida, ramas cortas, un PR por unidad de trabajo,
squash merge.

**Nunca ejecutar** `git commit`, `git push`, `git merge`, `git rebase`,
`git reset`, `git revert`, `git cherry-pick`, `git tag`, ni comandos `gh` que
escriban. Los de lectura (`status`, `diff`, `log`, `branch`, `show`) son libres.

Al terminar un bloque de trabajo, cerrar la respuesta con los comandos:

```
─── COMANDOS ───
git checkout -b feat/12-crud-cliente
git add src/main/java/.../ClienteController.java
git commit -m "feat(cliente): agregar endpoints de alta y consulta" -m "Refs #12"
git push -u origin feat/12-crud-cliente
```

Listar los archivos explícitamente. Nunca `git add .` ni `git add -A`.

### Commits

```
tipo(scope): descripción en imperativo

Cuerpo opcional: POR QUÉ se hizo así.

Refs #12
```

Tipos: `feat` `fix` `chore` `docs` `test` `refactor` `perf` `ci` `build`.
Imperativo, minúscula, sin punto final, máximo 72 caracteres.
`Refs #N` en commits, `Closes #N` en la descripción del PR.
Sin líneas de atribución ni co-autoría.

---

## Trabajo

- Proponer el enfoque y esperar aprobación antes de escribir código extenso.
- Los tests se escriben junto con el código, nunca después.
- No agregar dependencias sin justificar por qué no alcanza con lo existente.
- Si un PR pasa de ~400 líneas de diff, proponer cómo partirlo.
- Cuando una decisión tenga alternativas razonables, nombrarlas y decir por qué
  se eligió esta. Ese razonamiento va al Javadoc o al README, no se pierde en el chat.

---

## No tocar sin avisar

- `.github/workflows/`
- Versiones en `pom.xml`
- `README.md`

---

@~/.claude/tienda-api-personal.md
