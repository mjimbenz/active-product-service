package com.bank.active_product.controller;

import com.bank.active_product.api.ActiveProductApiDelegate;
import com.bank.active_product.api.model.ActiveProduct;
import com.bank.active_product.model.ActiveProductEntity;
import com.bank.active_product.service.ActiveProductService;
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
/*this.id = id;
    this.customerId = customerId;
    this.productType = productType;
    this.creditLimit = creditLimit;
    this.balance = balance;
    this.active = active;
    */
    private ActiveProduct toModel(ActiveProductEntity activeProductEntity) {
        return new ActiveProduct()
                .id(activeProductEntity.getId())
                .customerId(activeProductEntity.getCustomerId())
                .productType(ActiveProduct.ProductTypeEnum.fromValue(activeProductEntity.getProductType()))
                .creditLimit(BigDecimal.valueOf(activeProductEntity.getCreditLimit()))
                .balance(BigDecimal.valueOf(activeProductEntity.getBalance()))
                .active(activeProductEntity.isActive());
    }
}
