package com.bank.active_product.repository;

import com.bank.active_product.model.ActiveProductEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ActiveProductRepository extends ReactiveMongoRepository<ActiveProductEntity, String> {

    Mono<ActiveProductEntity> findByIdAndActiveTrue(String id);

    Flux<ActiveProductEntity> findByCustomerIdAndActiveTrue(String customerId);

    Mono<Long> countByCustomerIdAndProductTypeAndActiveTrue(String customerId, String productType);

}
