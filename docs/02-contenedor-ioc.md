# El contenedor IoC y el ciclo de vida de un bean

Documento de referencia para todo lo que en el resto del código se da por
sentado: "es un bean", "es singleton", "se inyecta por constructor". Acá se
explica una vez, con detalle, y se linkea desde ahí en vez de repetirlo.

Complementa [`01-anatomia-de-una-peticion.md`](01-anatomia-de-una-peticion.md):
ese documento sigue una petición HTTP en runtime; este documenta lo que pasa
**antes**, al arrancar la aplicación, para que esos beans existan.

## 1. Arranque: de clases a `BeanDefinition`

Cuando `ApiStorageApplication.main` llama a `SpringApplication.run`, lo
primero que pasa no es instanciar nada — es **escanear**. `@SpringBootApplication`
incluye `@ComponentScan`, que le dice al contenedor: explorá el paquete de
esta clase y todos los subpaquetes, buscando clases anotadas con
`@Component` (o cualquier meta-anotación que a su vez lleve `@Component`:
`@Service`, `@Repository`, `@Controller`, `@RestController`,
`@Configuration`).

Por cada clase candidata, el contenedor no crea un objeto todavía. Crea un
`BeanDefinition`: una descripción — nombre del bean, tipo, scope,
dependencias declaradas, callbacks de ciclo de vida — que se guarda en el
`BeanDefinitionRegistry`. Es metadata, no instancia. Esta separación importa
porque todo lo que sigue en las secciones 2 y 3 puede modificar esas
definiciones *antes* de que exista un solo objeto real.

## 2. Post-procesado de definiciones y autoconfiguración

Antes de instanciar nada, el contenedor le da la oportunidad a los
`BeanFactoryPostProcessor` de tocar las `BeanDefinition` ya registradas — o
registrar nuevas. Dos casos concretos que corren en cualquier app Spring
Boot:

- **`ConfigurationClassPostProcessor`**: procesa las clases `@Configuration`
  (como `OpenApiConfig` de este proyecto) y convierte cada método `@Bean`
  en una `BeanDefinition` adicional. Hasta este momento, un método `@Bean`
  no es más que una entrada en el registro — no se ejecutó ni una línea de
  su cuerpo.
- **Autoconfiguración de Spring Boot**: `@EnableAutoConfiguration` (incluida
  en `@SpringBootApplication`) dispara `AutoConfigurationImportSelector`,
  que lee la lista de clases de autoconfiguración del classpath (hoy vía
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`)
  y las agrega como `BeanDefinition` candidatas — pero cada una envuelta en
  condiciones: `@ConditionalOnClass`, `@ConditionalOnMissingBean`,
  `@ConditionalOnProperty`. Por eso `management.endpoints.web.exposure.include`
  en `application.properties` puede cambiar qué beans de actuator terminan
  existiendo sin tocar una línea de Java: la condición se evalúa acá, sobre
  la definición, no sobre un objeto ya creado.

Al final de esta etapa, el `BeanDefinitionRegistry` tiene la lista completa
y final de qué beans van a existir. Recién ahí arranca la instanciación.

## 3. Instanciación, inyección y `BeanPostProcessor`

Para cada `BeanDefinition` (salvo las de scope `prototype`, ver sección 5,
que se instancian bajo demanda), el contenedor:

1. **Resuelve el constructor** a usar. Con un solo constructor declarado
   (el caso de todas las clases de este proyecto, ej. `OpenApiConfig`), no
   hace falta `@Autowired` — Spring lo usa igual, porque no hay ambigüedad
   posible. Con varios constructores, hay que marcar uno con `@Autowired`
   para desambiguar.
2. **Resuelve cada parámetro del constructor** buscando en el propio
   `BeanDefinitionRegistry` un bean del tipo pedido (por tipo, y si hay más
   de un candidato, por nombre o por `@Qualifier`). Si esa dependencia
   todavía no fue instanciada, el contenedor la instancia primero,
   recursivamente — así se resuelve el grafo de dependencias.
3. **Invoca el constructor** con los argumentos resueltos. Acá recién existe
   un objeto Java real.
4. Corre los `BeanPostProcessor` registrados, en dos momentos:
   - `postProcessBeforeInitialization`, antes de los callbacks de
     inicialización (`@PostConstruct`, `afterPropertiesSet` de
     `InitializingBean`).
   - `postProcessAfterInitialization`, después. **Acá es donde se crean los
     proxies AOP** (sección 4) — el objeto que termina viviendo en el
     contenedor y que se inyecta en todos los demás beans puede no ser el
     que salió del constructor en el paso 3, sino un proxy que lo envuelve.

El orden de instanciación lo determina el grafo de dependencias, no el orden
en que las clases aparecen en el código: si `A` depende de `B`, `B` se
instancia primero, sin importar en qué paquete o archivo esté.

## 4. Proxies y por qué `this.metodo()` no atraviesa `@Transactional`

Cuando una clase tiene un método anotado `@Transactional` (o `@Cacheable`,
`@Async`, cualquier anotación de las que Spring implementa vía AOP), el
`BeanPostProcessor` correspondiente (`InfrastructureAdvisorAutoProxyCreator`,
registrado automáticamente por `@EnableTransactionManagement` — que
Spring Boot activa solo con tener un `PlatformTransactionManager` en el
contexto) no devuelve el objeto original en `postProcessAfterInitialization`.
Devuelve un **proxy**: un objeto nuevo, del mismo tipo (o que implementa las
mismas interfaces), que envuelve al original.

Ese proxy es el que el contenedor guarda y el que se inyecta en cualquier
otro bean que dependa de esta clase. Cuando alguien llama a
`pedidoService.confirmar(pedido)` desde **afuera**, la llamada pasa primero
por el proxy, que:

1. Abre una transacción (`TransactionInterceptor`).
2. Invoca el método real.
3. Hace commit o rollback según el resultado.

El problema aparece cuando el método transaccional se llama **desde adentro
de la misma clase**, con `this.metodo()`. `this` es una referencia al objeto
real, no al proxy — el proxy nunca estuvo en el medio de esa llamada. Java
resuelve `this.metodo()` como una invocación directa, virtual, sobre el
objeto concreto. No hay ninguna forma de interceptarla desde fuera de la
JVM sin bytecode weaving (que Spring no hace por defecto).

```java
@Service
public class PedidoService {

    public void procesarLote(List<Pedido> pedidos) {
        for (Pedido pedido : pedidos) {
            this.confirmar(pedido); // NO pasa por el proxy: sin transacción
        }
    }

    @Transactional
    public void confirmar(Pedido pedido) { ... }
}
```

Formas reales de evitarlo: mover `confirmar` a otro bean e inyectarlo (la
llamada entre beans sí atraviesa el proxy del segundo), o inyectarse a sí
mismo por constructor (`private final PedidoService self`, que Spring
resuelve al proxy) y llamar `self.confirmar(pedido)`. La primera opción es
casi siempre la mejor señal de que el método debería vivir en otra clase.

**Dos tipos de proxy**, y por qué importa: si la clase implementa una
interfaz, Spring usa un **JDK dynamic proxy** (implementa esa interfaz por
reflection). Si no implementa ninguna, usa **CGLIB** (genera una subclase en
runtime) — por eso una clase con métodos `final` o un constructor privado no
se puede proxyear con CGLIB: no se puede heredar de ella.

## 5. `@Component`, `@Service`, `@Repository`, `@Controller`: diferencias reales

Las cuatro son *meta-anotadas* con `@Component` — para el mecanismo de
component-scanning de la sección 1, son indistinguibles: las cuatro
producen un `BeanDefinition`, singleton por defecto. La diferencia entre
`@Service` y `@Component`, por ejemplo, es **puramente documental**: le dice
a quien lee el código en qué capa vive esa clase (ver la regla de Javadoc
en `CLAUDE.md`). Elegir el estereotipo correcto es documentación ejecutable,
no una decisión que cambie el comportamiento del contenedor.

La excepción real es `@Repository`: además de registrar el bean, habilita
que el `PersistenceExceptionTranslationPostProcessor` (otro
`BeanPostProcessor`) envuelva la clase para traducir excepciones específicas
del proveedor de persistencia (`SQLException`, excepciones de Hibernate) a
la jerarquía `DataAccessException` de Spring, agnóstica de la tecnología.
Es la única de las cuatro con una diferencia de comportamiento, no solo de
intención.

`@Controller` (y `@RestController`, que le suma `@ResponseBody` — ver
Javadoc de `GreetingController`) es la que usa
`RequestMappingHandlerMapping` para construir el mapa de rutas: solo las
clases con este estereotipo (o `@RestController`) se inspeccionan buscando
métodos `@RequestMapping`/`@GetMapping`/etc.

### Cuándo usar `@Bean` en una clase `@Configuration` en vez de un estereotipo

`@Bean` (como `OpenApiConfig.apiStorageOpenApi()`) hace falta cuando:

- **La clase no es tuya**: no podés poner `@Component` en una clase de una
  librería de terceros (`OpenAPI`, del SDK de springdoc). `@Bean` es la
  única forma de que ese objeto entre al contenedor.
- **La construcción necesita lógica**, no solo inyección de dependencias:
  ramas condicionales, valores calculados, builders con varios pasos — ver
  el fallback a `NO_BUILD_INFO_VERSION` en `OpenApiConfig` cuando
  `BuildProperties` no está disponible. Eso no se puede expresar con un
  constructor anotado `@Component`.
- **Necesitás más de una instancia del mismo tipo** con distinta
  configuración (dos `RestClient` apuntando a hosts distintos, por
  ejemplo) — con `@Component` solo hay una clase, y una clase es una
  `BeanDefinition`.

## 6. Scopes disponibles y el default

| Scope | Instancias | Cuándo usarlo |
|---|---|---|
| `singleton` (**default**) | Una por contenedor, compartida por toda la app. | El caso normal: controllers, services, repositories — todo lo de este proyecto hasta ahora. |
| `prototype` | Una nueva cada vez que se pide/inyecta. | Objetos con estado mutable por uso, que no pueden compartirse entre hilos. |
| `request` | Una por petición HTTP. | Datos que solo tienen sentido durante una request (requiere contexto web). |
| `session` | Una por sesión HTTP. | Estado de usuario entre requests (requiere contexto web). |
| `application` | Una por `ServletContext`. | Poco común; casi equivalente a singleton en una app de un solo módulo web. |

Que el default sea `singleton` es la razón por la que en este proyecto (ver
Javadoc de `GreetingController` y `OpenApiConfig`) se insiste tanto en que
las clases no tengan estado de instancia mutable: una sola instancia
atiende **todas** las peticiones concurrentes, en distintos hilos del pool
de Tomcat. Un campo de instancia no `final` ahí es una condición de carrera
esperando a pasar en producción bajo carga.

Inyectar un bean de scope más corto (`prototype`, `request`) dentro de un
singleton es un problema clásico: el singleton se construye una sola vez,
así que "atraparía" siempre la misma instancia del bean de scope corto. La
solución es un *scoped proxy* (`proxyMode = ScopedProxyMode.TARGET_CLASS`):
el singleton recibe un proxy liviano que, en cada llamada, resuelve la
instancia correcta del scope corto — no aplica a este proyecto todavía
(no hay nada de scope `request` ni `prototype`), pero es la pieza que
faltaría si apareciera.

## 7. Por qué la inyección por constructor es preferible

Frente a inyección por campo (`@Autowired` en un field) o por setter,
constructor:

- **Hace las dependencias explícitas e inmutables**: un campo `final`
  solo es posible con inyección por constructor. Cualquiera que lea la
  clase ve, en una sola firma, todo lo que necesita para existir — no hay
  que leer el cuerpo entero buscando campos `@Autowired`.
- **Falla rápido y en el lugar correcto**: si falta una dependencia, el
  contenedor lo detecta al construir el bean, en el arranque de la
  aplicación, con un mensaje que dice exactamente qué constructor y qué
  parámetro. Con inyección por campo, el objeto se construye igual (el
  campo queda `null`) y el error aparece más tarde, como
  `NullPointerException`, en el momento en que alguien intenta usar esa
  dependencia — potencialmente en producción, lejos del código que causó
  el problema.
- **Es testeable sin levantar contenedor**: `new PedidoService(mockRepo)`
  compila y corre sin Spring — exactamente el patrón de
  `OpenApiConfigUnitTest` en este proyecto. Con campos privados inyectados
  por Spring, hay que recurrir a reflection o a un contexto de test
  (`@SpringBootTest`) para inyectar los mocks, mucho más lento y con más
  superficie de fallo.
- **Expone las dependencias circulares en vez de esconderlas**: si `A`
  necesita a `B` y `B` necesita a `A` por constructor, el contenedor no
  puede resolver el orden de instanciación y falla en el arranque, con un
  mensaje claro. Con inyección por campo, Spring puede resolver el ciclo
  igual (crea el objeto vacío primero, inyecta los campos después) —
  ocultando un problema de diseño real detrás de un mecanismo que
  "funciona".

Por estas razones es la única forma de inyección que se usa en este
repositorio (ver `CLAUDE.md`, sección de arquitectura).
