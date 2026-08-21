# Anatomía de una petición

Recorrido completo de `GET /api/v1/saludo?nombre=Rodolfo`, desde que el
primer byte llega al socket hasta que el JSON sale por la red. Cada paso
referencia la clase real involucrada, no una versión simplificada.

## 1. El socket: Tomcat embebido

`spring-boot-starter-webmvc` trae `spring-boot-tomcat`, que arranca un
servidor Tomcat embebido dentro del mismo proceso de la JVM — no es un
Tomcat externo al que se le despliega un `.war`. Al iniciar
`ApiStorageApplication`, Spring Boot crea un `TomcatServletWebServerFactory`
que levanta un `Connector` escuchando en el puerto configurado (8080 por
defecto).

Cuando llega la petición TCP, el `Connector` la parsea como HTTP/1.1: método,
path, headers, body. Ese parseo produce un `org.apache.catalina.connector.Request`
de Tomcat, que Tomcat adapta a un `jakarta.servlet.http.HttpServletRequest` —
la abstracción estándar de Servlet API, la misma que usaría cualquier
aplicación Java EE/Jakarta EE, no algo propietario de Spring.

## 2. El front controller: `DispatcherServlet`

Tomcat no le pasa el request directo a un método de negocio. Hay un único
servlet registrado para todo (`/`): el `DispatcherServlet` de Spring MVC.
Es el patrón *front controller* — todo entra por un solo punto, que decide
a quién delegar. `DispatcherServlet` se registra como bean vía
`DispatcherServletAutoConfiguration`, con scope singleton: una instancia
atiende todas las peticiones de la aplicación.

`DispatcherServlet.doDispatch(request, response)` es el método que orquesta
todo lo que sigue.

## 3. Encontrar el método: `HandlerMapping`

`DispatcherServlet` le pregunta a la cadena de `HandlerMapping` registrados
"¿quién atiende esta petición?". La que importa acá es
`RequestMappingHandlerMapping`, que en el arranque de la aplicación escaneó
todos los beans `@RestController`/`@Controller` y armó un mapa de
`(método HTTP, path) → HandlerMethod`.

Para `GET /api/v1/saludo`, ese mapa tiene una entrada que apunta a
`GreetingController.greet(String)`, registrada porque el método está
anotado con `@GetMapping("/api/v1/saludo")`. El resultado de esta etapa es
un `HandlerExecutionChain`: el método a invocar más la cadena de
interceptors que corren antes y después (acá no hay ninguno configurado).

## 4. Invocar el método: `HandlerAdapter` y resolución de parámetros

`DispatcherServlet` no invoca el método directamente — delega en un
`HandlerAdapter` (`RequestMappingHandlerAdapter` para métodos anotados con
`@RequestMapping`/`@GetMapping`). Este adapter resuelve cada parámetro del
método usando la cadena de `HandlerMethodArgumentResolver`.

Para el parámetro Java `name`, anotado
`@RequestParam(name = "nombre", defaultValue = "Mundo")`, entra en juego
`RequestParamMethodArgumentResolver`: busca `nombre` en la query string del
request (el nombre HTTP del parámetro, distinto del identificador Java);
si está, usa ese valor; si no está, usa el `defaultValue`. Con eso arma el
argumento y llama a `greet("Rodolfo")` (o `greet("Mundo")` si no vino el
query param).

## 5. El método de negocio (acá, trivial)

`GreetingController.greet` corre en el hilo que Tomcat le asignó a esta
petición (un hilo del pool de Tomcat, no el hilo principal de la app).
Construye y devuelve un `GreetingResponse` — un record, un objeto Java común,
sin nada de HTTP en el medio.

## 6. De objeto a bytes: `HttpMessageConverter`

Acá es donde entra `@ResponseBody` (implícito por `@RestController`, ver
Javadoc de `SaludoController`). Como el método no devuelve un nombre de
vista, el `HandlerMethodReturnValueHandler` correspondiente
(`HttpEntityMethodProcessor`/`RequestResponseBodyMethodProcessor`) toma el
`SaludoResponse` devuelto y busca, entre los `HttpMessageConverter`
registrados, uno que pueda escribirlo según el header `Accept` de la
petición.

Con `spring-boot-starter-webmvc` en el classpath, Jackson está disponible y
Spring Boot registra `MappingJackson2HttpMessageConverter` automáticamente.
Ese converter serializa el record a JSON usando reflection sobre sus
accesores (`message()`) y escribe el resultado en el `OutputStream` del
`HttpServletResponse`, además de setear el header
`Content-Type: application/json`.

## 7. De vuelta al socket

`HttpServletResponse` es, otra vez, la abstracción de Servlet API — Tomcat
toma esos bytes, arma la respuesta HTTP/1.1 completa (status line, headers,
body) y la escribe en el socket TCP de vuelta al cliente.

## El recorrido completo, resumido

```
byte TCP → Tomcat (Connector) → HttpServletRequest
    → DispatcherServlet.doDispatch
        → RequestMappingHandlerMapping   (¿qué método atiende esto?)
        → RequestMappingHandlerAdapter   (resolver @RequestParam, invocar)
            → GreetingController.greet(String name)
        → HttpMessageConverter           (SaludoResponse → JSON)
    → HttpServletResponse
→ Tomcat (Connector) → byte TCP
```

Ninguna de estas piezas es específica de este endpoint — es el mismo
recorrido para cualquier `@RestController` de la aplicación. Lo único que
cambia entre endpoints son los pasos 3 (qué método matchea) y 5 (qué hace
ese método).

   

