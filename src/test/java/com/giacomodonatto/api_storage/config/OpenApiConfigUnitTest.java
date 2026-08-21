package com.giacomodonatto.api_storage.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

/**
 * Prueba unitaria de {@link OpenApiConfig}, sin levantar contexto de Spring
 * (posible porque la dependencia se inyecta por constructor). Cubre el
 * fallback de versión cuando {@link BuildProperties} no está disponible —
 * el caso real que rompía el arranque de la app desde el IDE antes de
 * cambiar a {@link ObjectProvider}.
 */
class OpenApiConfigUnitTest {

    @Test
    void usaLaVersionDeBuildPropertiesCuandoEstaDisponible() {
        BuildProperties buildProperties = mock(BuildProperties.class);
        when(buildProperties.getVersion()).thenReturn("1.2.3");

        OpenAPI openApi = new OpenApiConfig(providerCon(buildProperties)).apiStorageOpenApi();

        assertThat(openApi.getInfo().getVersion()).isEqualTo("1.2.3");
    }

    @Test
    void caeAVersionPlaceholderCuandoBuildPropertiesNoExiste() {
        OpenAPI openApi = new OpenApiConfig(providerCon(null)).apiStorageOpenApi();

        assertThat(openApi.getInfo().getVersion()).isEqualTo(OpenApiConfig.VERSION_SIN_BUILD_INFO);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<BuildProperties> providerCon(BuildProperties value) {
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
