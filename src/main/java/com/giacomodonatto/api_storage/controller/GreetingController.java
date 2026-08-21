package com.giacomodonatto.api_storage.controller;

import com.giacomodonatto.api_storage.dto.GreetingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de saludo: el "hola mundo" de la API. Existe como excusa para
 * documentar el recorrido completo de una petición HTTP a través del
 * framework — ver {@code docs/01-anatomia-de-una-peticion.md} para el
 * detalle byte a byte, desde el socket de Tomcat hasta el JSON de salida.
 *
 * <p>Anotada con {@code @RestController} y no con {@code @Controller}
 * porque {@code @RestController} es exactamente {@code @Controller} +
 * {@code @ResponseBody} aplicado a nivel de clase. Con solo
 * {@code @Controller}, Spring MVC interpreta lo que devuelve cada método
 * como el nombre lógico de una vista que hay que resolver (un template
 * Thymeleaf, un JSP) — acá no hay vistas, cada método devuelve el objeto que
 * tiene que ir directo al cuerpo de la respuesta. Sin {@code @ResponseBody}
 * (que {@code @RestController} agrega implícitamente a todos los métodos),
 * Spring intentaría resolver algo como
 * {@code "GreetingResponse[message=Hola, Mundo!]"} como nombre de vista y
 * respondería 404 en lugar de servir el JSON.
 *
 * <p>Lo que agrega puntualmente {@code @ResponseBody}: le dice al
 * {@code HandlerMethodReturnValueHandler} de Spring MVC que el valor de
 * retorno del método es el cuerpo de la respuesta, no un nombre de vista, y
 * que por lo tanto tiene que pasar por un
 * {@link org.springframework.http.converter.HttpMessageConverter} —
 * concretamente {@code MappingJackson2HttpMessageConverter}, ya en el
 * classpath vía {@code spring-boot-starter-webmvc} — para serializarse según
 * el header {@code Accept} de la petición.
 *
 * <p>Scope singleton, el de por defecto: el contenedor crea una sola
 * instancia de este controller y la reutiliza para atender todas las
 * peticiones concurrentes. Es seguro porque la clase no guarda estado de
 * instancia — el parámetro {@code name} de cada request vive en la pila
 * del hilo que ejecuta {@link #greet(String)}, nunca en un campo
 * compartido.
 */
@RestController
public class GreetingController {

    private static final String DEFAULT_NAME = "Mundo";

    /**
     * Devuelve un saludo, personalizado si se manda un nombre.
     *
     * <p>Mapeado a {@code GET /api/v1/saludo}. El {@code DispatcherServlet}
     * delega en el {@code RequestMappingHandlerMapping} para encontrar este
     * método a partir del path y el verbo HTTP de la petición; el recorrido
     * completo está documentado en
     * {@code docs/01-anatomia-de-una-peticion.md}.
     *
     * <p>El parámetro Java se llama {@code name} (código en inglés), pero
     * se ata explícitamente al query param {@code nombre} vía
     * {@code @RequestParam(name = "nombre")} porque ese es el nombre de
     * parámetro que pide el contrato HTTP del endpoint — un detalle de
     * código no puede cambiar un contrato público.
     *
     * @param name nombre a incluir en el saludo, tomado del query param
     *             {@code nombre}. Si no se manda, vale
     *             {@value #DEFAULT_NAME}.
     * @return un {@link GreetingResponse} con el mensaje armado; Spring lo
     *         serializa a JSON en el cuerpo de una respuesta 200.
     */
    @GetMapping("/api/v1/saludo")
    public GreetingResponse greet(@RequestParam(name = "name", defaultValue = DEFAULT_NAME) String name) {
        return new GreetingResponse("Hola, " + name + "!");
    }
}
