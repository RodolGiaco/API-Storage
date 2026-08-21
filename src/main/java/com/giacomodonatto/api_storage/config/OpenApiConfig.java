package com.giacomodonatto.api_storage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentación OpenAPI expuesta por springdoc en
 * {@code /v3/api-docs} y renderizada por Swagger UI en {@code /swagger-ui.html}.
 *
 * <p>Anotada con {@code @Configuration} y no con {@code @Component} porque su
 * único propósito es declarar beans de infraestructura (acá, la descripción
 * de la API); no tiene lógica propia que ejecutar. springdoc detecta
 * automáticamente cualquier bean {@link OpenAPI} en el contexto y lo usa como
 * base del documento generado — sin este bean, springdoc igual arma un
 * documento válido, pero sin título, descripción, versión ni contacto.
 *
 * <p>Scope singleton (el de por defecto): una sola instancia de {@link OpenAPI}
 * se construye al arrancar el contexto y se reutiliza para cada request a
 * {@code /v3/api-docs}, porque los metadatos que describe no cambian en
 * tiempo de ejecución.
 *
 * <p>Depende de {@link BuildProperties}, inyectada por constructor pero
 * envuelta en {@link ObjectProvider} en lugar de recibirla directa. Ese bean
 * lo autoconfigura Spring Boot solo cuando existe
 * {@code META-INF/build-info.properties} en el classpath — generado por la
 * ejecución {@code build-info} del {@code spring-boot-maven-plugin} (ver
 * {@code pom.xml}). Ese goal corre en la fase {@code generate-resources}, así
 * que cualquier build hecho con Maven (jar empaquetado, CI,
 * {@code ./mvnw spring-boot:run}) lo tiene; pero el botón "Run" del IDE (o un
 * restart de devtools) usa su propio compilador incremental y nunca ejecuta
 * ese goal, así que el archivo no existe ahí. Con inyección directa la app
 * no arranca en ese escenario; con {@link ObjectProvider} se degrada a
 * {@link #VERSION_SIN_BUILD_INFO} en desarrollo local y usa la versión real
 * en todo lo demás, que es lo único que importa para el issue.
 */
@Configuration
public class OpenApiConfig {

    /** Versión mostrada cuando no corrió el build-info de Maven (ver arriba). */
    static final String VERSION_SIN_BUILD_INFO = "dev";

    private final ObjectProvider<BuildProperties> buildProperties;

    public OpenApiConfig(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    /**
     * Define título, descripción, versión y contacto de la API.
     *
     * <p>La versión se toma de {@link BuildProperties#getVersion()} en lugar
     * de escribirse acá a mano, para que quede sincronizada con la versión
     * declarada en {@code pom.xml} sin mantenimiento manual; si ese bean no
     * está disponible (ver Javadoc de la clase) cae a
     * {@link #VERSION_SIN_BUILD_INFO}.
     *
     * @return el documento {@link OpenAPI} con los metadatos de la API, que
     *         springdoc completa luego con los paths y schemas detectados en
     *         los controllers.
     */
    @Bean
    public OpenAPI apiStorageOpenApi() {
        BuildProperties properties = this.buildProperties.getIfAvailable();
        String version = (properties != null) ? properties.getVersion() : VERSION_SIN_BUILD_INFO;
        return new OpenAPI()
                .info(new Info()
                        .title("API Storage")
                        .description(
                                "API REST de una tienda: gestión de clientes, productos y pedidos.")
                        .version(version)
                        .contact(new Contact()
                                .name("RodolGiaco")
                                .email("rodol.giacomodonatto@gmail.com")));
    }
}
