package com.giacomodonatto.api_storage.dto;

/**
 * Respuesta del endpoint de saludo ({@code GET /api/v1/saludo}).
 *
 * <p>Record y no una clase con {@code @Data}: es un DTO de salida — sale de
 * la API, nunca se muta después de crearse — y un record da constructor
 * canónico, {@code equals}/{@code hashCode}/{@code toString} y accesores sin
 * depender de Lombok ni escribir código de más.
 *
 * @param message el saludo ya armado, por ejemplo {@code "Hola, Mundo!"}.
 */
public record GreetingResponse(String message) {
}
