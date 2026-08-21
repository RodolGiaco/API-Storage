package com.giacomodonatto.api_storage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code @WebMvcTest} en lugar de {@code @SpringBootTest}: levanta solo la
 * capa web (este controller, el {@code DispatcherServlet} y los
 * {@code HttpMessageConverter}), sin repositorios ni contexto completo — más
 * rápido, y alcanza porque acá no hay nada más que probar.
 */
@WebMvcTest(GreetingController.class)
class GreetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsGreetingWithDefaultNameWhenNoneProvided() throws Exception {
        mockMvc.perform(get("/api/v1/saludo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Hola, Mundo!"));
    }

    @Test
    void returnsGreetingWithProvidedName() throws Exception {
        mockMvc.perform(get("/api/v1/saludo").param("nombre", "Rodolfo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Hola, Rodolfo!"));
    }
}
