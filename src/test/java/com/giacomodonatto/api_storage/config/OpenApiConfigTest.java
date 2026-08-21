package com.giacomodonatto.api_storage.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica los criterios de aceptación del issue de Actuator + OpenAPI:
 * {@code /actuator/health} e {@code /actuator/info} responden y este último
 * expone build-info y commit; el resto de actuator queda cerrado; y
 * springdoc publica el documento OpenAPI con los metadatos definidos en
 * {@link OpenApiConfig}, con Swagger UI accesible.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealthEstaExpuesto() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test
    void actuatorInfoExponeVersionYCommit() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.build.version").exists())
                .andExpect(jsonPath("$.git").exists());
    }

    @Test
    void restoDeActuatorEstaCerrado() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
    }

    @Test
    void documentoOpenApiIncluyeLosMetadatosConfigurados() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API Storage"))
                .andExpect(jsonPath("$.info.contact.email").value("rodol.giacomodonatto@gmail.com"));
    }

    @Test
    void swaggerUiEsAccesible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
