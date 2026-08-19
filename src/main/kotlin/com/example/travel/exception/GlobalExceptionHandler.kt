package com.example.travel.exception

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Invalid request", "BAD_REQUEST", ex.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        val fieldErrors = ex.bindingResult.fieldErrors
            .associate { it.field to (it.defaultMessage ?: "invalid") }
        return problem(
            HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_FAILED",
            "One or more fields are invalid.",
        ).apply { setProperty("errors", fieldErrors) }
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "Not found", "NOT_FOUND", ex.message)

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ProblemDetail =
        problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", "INVALID_CREDENTIALS", ex.message)

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailExists(ex: EmailAlreadyExistsException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "Email already registered", "EMAIL_EXISTS", ex.message)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ProblemDetail =
        problem(HttpStatus.FORBIDDEN, "Access denied", "ACCESS_DENIED", ex.message)

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLock(ex: ObjectOptimisticLockingFailureException): ProblemDetail =
        problem(
            HttpStatus.CONFLICT, "Version conflict", "VERSION_CONFLICT",
            "This item was modified by someone else. Please reload and try again.",
        )

    @ExceptionHandler(PlacesUnavailableException::class, CallNotPermittedException::class)
    fun handlePlacesUnavailable(ex: Exception): ProblemDetail =
        problem(
            HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable",
            "PLACES_UNAVAILABLE", "Place lookup is temporarily unavailable. Please try again shortly."
        )

    private fun problem(
        status: HttpStatus,
        title: String,
        code: String,
        detail: String?,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail ?: title).apply {
            this.title = title
            setProperty("code", code)
        }
}