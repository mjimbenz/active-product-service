package com.bank.active_product.service;

import com.bank.active_product.model.ActiveProductEntity;
import jakarta.annotation.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ActiveProductService {
    public Flux<ActiveProductEntity> findAll(String customerId);

    public Mono<ActiveProductEntity> findById(String id);

    public Mono<ActiveProductEntity> create(ActiveProductEntity e);

    public Mono<ActiveProductEntity> update(String id, ActiveProductEntity e);

    public Mono<Void> delete(String id);

    public Mono<Double> getBalance(String id);

    Flux<ActiveProductEntity> findByCustomerId(String customerId, @Nullable String productId);
}
