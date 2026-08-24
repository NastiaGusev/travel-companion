package com.example.travel.exception

/**
 * Thrown when the AI extraction service (ai-service) cannot be reached or fails.
 *
 * Deliberately distinct from [PlacesUnavailableException]/the raw HttpServerErrorException
 * family: both AiExtractionClient and PlacesClient go through RestClient + Resilience4j, so
 * their raw failures share the same exception TYPES (HttpServerErrorException,
 * ResourceAccessException, CallNotPermittedException). QuickAddService catches those raw types
 * at the call site — outside the @Retry/@CircuitBreaker-annotated method, so retry/breaker still
 * see them — and rethrows this type, giving GlobalExceptionHandler an unambiguous signal for
 * which downstream actually failed.
 */
class AiServiceUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
