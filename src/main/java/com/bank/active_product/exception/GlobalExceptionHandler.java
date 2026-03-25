package com.bank.active_product.exception;

import com.bank.active_product.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MICROSERVICE_NAME = "active-product-service";

    private ErrorResponse buildError(String code, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .errorCode(code)
                .message(message)
                .path(path)
                .microservice(MICROSERVICE_NAME)
                .build();
    }

    private Mono<ResponseEntity<ErrorResponse>> buildResponse(
            HttpStatus status, String code, String message, String path) {

        return Mono.just(
                ResponseEntity.status(status)
                        .body(buildError(code, message, path))
        );
    }

    // -----------------------------------------------------
    // 1. BUSINESS EXCEPTION
    // -----------------------------------------------------
    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessException(
            BusinessException ex, ServerWebExchange exchange) {

        var path = exchange.getRequest().getPath().value();

        log.warn("[BusinessException] {} | path={}", ex.getMessage(), path);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "BUSINESS_ERROR",
                ex.getMessage(),
                path
        );
    }

    // -----------------------------------------------------
    // 2. DECODING EXCEPTION (JSON mal formado)
    // -----------------------------------------------------
    @ExceptionHandler(org.springframework.core.codec.DecodingException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDecodingException(
            Exception ex, ServerWebExchange exchange) {

        var cause = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        var path = exchange.getRequest().getPath().value();

        log.error("[DecodingException] Causa: {} | path={}", cause, path);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "El formato del cuerpo enviado es inválido. Verifica tipos, nombre de campos y ENUMs.",
                path
        );
    }

    // -----------------------------------------------------
    // 3. VALIDATION EXCEPTION (Bean Validation)
    // -----------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            MethodArgumentNotValidException ex, ServerWebExchange exchange) {

        final String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(field -> field.getField() + " " + field.getDefaultMessage())
                .collect(Collectors.joining("; "));

        var path = exchange.getRequest().getPath().value();

        log.warn("[ValidationException] {} | path={}", errors, path);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                errors,
                path
        );
    }

    // -----------------------------------------------------
    // 4. ERRORES DE SERVICIOS EXTERNOS (WebClient)
    // -----------------------------------------------------
    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebClientError(
            WebClientResponseException ex, ServerWebExchange exchange) {

        var path = exchange.getRequest().getPath().value();

        log.error("[ExternalServiceError] status={} body='{}' path={}",
                ex.getStatusCode(),
                ex.getResponseBodyAsString(),
                path
        );

        return buildResponse(
                (HttpStatus) ex.getStatusCode(),
                "EXTERNAL_SERVICE_ERROR",
                ex.getStatusText(),
                path
        );
    }

    // -----------------------------------------------------
    // 5. ERRORES DESCONOCIDOS (CAJA NEGRA)
    // -----------------------------------------------------
    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericError(
            Throwable ex, ServerWebExchange exchange) {

        var cause = ex.getCause() != null ? ex.getCause().getMessage() : "none";
        var path = exchange.getRequest().getPath().value();

        log.error("[UnexpectedError] {} | cause={} | path={}",
                ex.getMessage(), cause, path, ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Ocurrió un error inesperado en el servicio.",
                path
        );
    }
}