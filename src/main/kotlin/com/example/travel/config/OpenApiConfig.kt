package com.example.travel.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.Components
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI {
        val schemeName = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("AI Travel Companion API")
                    .description("Backend service for planning trips, itineraries, and stops.")
                    .version("v1"),
            )
            // Declares a JWT bearer scheme so the Swagger UI shows an "Authorize" button
            .addSecurityItem(SecurityRequirement().addList(schemeName))
            .components(
                Components().addSecuritySchemes(
                    schemeName,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )
    }
}