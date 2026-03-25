package com.bank.active_product.controller;

import com.bank.active_product.api.ActiveProductApiDelegate;
import com.bank.active_product.api.model.ActiveProduct;
import com.bank.active_product.api.model.ActiveProductRequest;
import com.bank.active_product.exception.BusinessException;
import com.bank.active_product.model.ActiveProductEntity;
import com.bank.active_product.service.ActiveProductService;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActiveProductServiceApiDelegateImpl implements ActiveProductApiDelegate {

    private final ActiveProductService service;

    @Override
    public Mono<ResponseEntity<Flux<ActiveProduct>>> rootGet(String customerId, ServerWebExchange exchange) {
        log.info("[api] Getting active products for customerId={}", customerId);
        return Mono.just(ResponseEntity.ok(service.findAll(customerId).map(this::toModel)))
                .doOnSuccess(c -> log.info("[api] Successfully retrieved active products for customerId={}", customerId))
                .onErrorResume(err -> {
                    log.error("[api] Error retrieving active products for customerId={}, error={}", customerId, err.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    @Override
    public Mono<ResponseEntity<ActiveProduct>> rootPost(Mono<ActiveProductRequest> activeProductRequest, ServerWebExchange exchange) {
        log.info("[api] Creating active product -> {}", activeProductRequest);
        return activeProductRequest.map(this::toEntity)
                .flatMap(service::create)
                .doOnSuccess(c -> log.info("[api] Successfully created active product for customerId={}", c.getCustomerId()))
                .doOnError(err -> log.error("[api] Error creating active product, error={}", err.getMessage()))
                .map(this::toModel)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> idPut(
            String id,
            @Valid Mono<ActiveProductRequest> activeProductRequest,
            ServerWebExchange exchange) {

        return activeProductRequest
                .map(this::toEntity)
                .flatMap(e -> service.update(id, e))
                .doOnSuccess(updated ->
                        log.info("[api] Successfully updated active product with id={}", id)
                )
                .map(updated ->
                        ResponseEntity.noContent().<Void>build()
                )
                .onErrorResume(err -> {
                    log.error("[api] Error updating active product with id={}, error={}", id, err.getMessage());

                    if (err instanceof BusinessException) {
                        return Mono.just(ResponseEntity.badRequest().build());
                    }
                    if (err instanceof NotFoundException) {
                        return Mono.just(ResponseEntity.notFound().build());
                    }

                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    @Override
    public Mono<ResponseEntity<ActiveProduct>> idGet(String id, ServerWebExchange exchange) {

        log.info("[api] Getting active product by id={}", id);

        return service.findById(id)
                .map(this::toModel)
                .map(ResponseEntity::ok)
                .doOnSuccess(r ->
                        log.info("[api] Successfully retrieved active product with id={}", id)
                )
                .onErrorResume(err -> {
                    log.error("[api] Error retrieving active product with id={}, error={}",
                            id, err.getMessage());

                    if (err instanceof NotFoundException) {
                        return Mono.just(ResponseEntity.notFound().build());
                    }

                    if (err instanceof BusinessException) {
                        return Mono.just(ResponseEntity.badRequest().build());
                    }

                    // Error inesperado → 500
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    @Override
    public Mono<ResponseEntity<Void>> idDelete(String id, ServerWebExchange exchange) {
        log.info("[api] Deleting active product with id={}", id);
        return service.delete(id)
                .doOnSuccess(c -> log.info("[api] Successfully deleted active product with id={}", id))
                .doOnError(err -> log.error("[api] Error deleting active product with id={}, error={}", id, err.getMessage()))
                .thenReturn(ResponseEntity.noContent().build());
    }



    private ActiveProduct toModel(ActiveProductEntity activeProductEntity) {
        return new ActiveProduct()
                .id(activeProductEntity.getId())
                .customerId(activeProductEntity.getCustomerId())
                .productType(ActiveProduct.ProductTypeEnum.fromValue(activeProductEntity.getProductType()))
                .creditLimit(BigDecimal.valueOf(activeProductEntity.getCreditLimit()))
                .balance(BigDecimal.valueOf(activeProductEntity.getBalance()))
                .createdAt(activeProductEntity.getCreatedAt() != null ? activeProductEntity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .updatedAt(activeProductEntity.getUpdatedAt() != null ? activeProductEntity.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .deletedAt(activeProductEntity.getDeletedAt() != null ? activeProductEntity.getDeletedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .active(activeProductEntity.isActive());
    }

    private ActiveProductEntity toEntity(ActiveProductRequest activeProduct){
        return ActiveProductEntity.builder()
                .customerId(activeProduct.getCustomerId())
                .productType(activeProduct.getProductType().getValue())
                .build();
    }
}
