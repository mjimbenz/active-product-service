package com.bank.active_product.service.impl;

import com.bank.active_product.client.CustomerWebClient;
import com.bank.active_product.client.entity.Customer;
import com.bank.active_product.exception.BusinessException;
import com.bank.active_product.model.ActiveProductEntity;
import com.bank.active_product.repository.ActiveProductRepository;
import com.bank.active_product.service.ActiveProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActiveProductServiceImpl implements ActiveProductService {

    private final ActiveProductRepository repository;
    private final CustomerWebClient customerWebClient;
    private final String PERSONAL_CREDIT="PERSONAL_CREDIT";
    private final String BUSINESS_CREDIT="BUSINESS_CREDIT";
    private final String CREDIT_CARD="CREDIT_CARD";
    private final String BUSINESS="BUSINESS";
    private final String PERSONAL="PERSONAL";



    // =====================================================
    // LIST ALL ACTIVE PRODUCT FOR CUSTOMER ID
    // =====================================================

    @Override
    public Flux<ActiveProductEntity> findAll(String customerId) {
        log.info("[Service] Listing all active passive products - traceId={}");
        if(customerId != null){
            return validateCustomerExists(customerId)
                    .doOnSuccess(c -> log.info("[Service] Customer validated: {}", customerId))
                    .doOnError(err -> log.error("[Service] Customer validation failed for id={}, error={}", customerId, err.getMessage()))
                    .flatMapMany(customer -> {
                                return repository.findByCustomerIdAndActiveTrue(customerId)
                                        .doOnNext(product -> log.info("[Service] Found active product for customer {}: {}", customerId, product.getId()))
                                        .doOnError(err -> log.error("[Service] Error fetching active products for customer {}, error={}", customerId, err.getMessage()));
                            }
                    )
                    .onErrorResume(err -> {
                        log.error("[Service] Error in findByCustomerId, customerId={}, error={}", customerId, err.getMessage());
                        return Flux.error(err);
                    });
        }
        return repository.findAll()
                .filter(ActiveProductEntity::isActive)
                .doOnNext(product -> log.info("[Service] Found active product: {}", product.getId()))
                .doOnError(err -> log.error("[Service] Error fetching active products, error={}", err.getMessage()))
                .onErrorResume(err -> {
                    log.error("[Service] Error in findAll, error={}", err.getMessage());
                    return Flux.error(err);
                });


    }
    // =====================================================
    //FIND ACTIVE PRODUCT BY ID
    // =====================================================

    @Override
    public Mono<ActiveProductEntity> findById(String id) {
        log.info("[Service] Finding active product by id={}, traceId={}", id);
        return repository.findById(id)
                .doOnSuccess(product -> {
                    if (product != null) {
                        log.info("[Service] Found active product: {}", product.getId());
                    } else {
                        log.warn("[Service] No active product found with id={}", id);
                    }
                })
                .doOnError(err -> log.error("[Service] Error finding active product by id={}, error={}", id, err.getMessage()))
                .onErrorResume(err -> {
                    log.error("[Service] Error in findById, id={}, error={}", id, err.getMessage());
                    return Mono.error(err);
                });
    }

    // =====================================================
    // CREATE ACTIVE PRODUCT + RULES
    // =====================================================
    @Override
    public Mono<ActiveProductEntity> create(ActiveProductEntity e) {
        log.info("[Service] Creating active product for customerId={}, productType={}, traceId={}",
                e.getCustomerId(), e.getProductType());

        e.setActive(true);
        e.setCreatedAt(LocalDateTime.now());

        return validateCustomerExists(e.getCustomerId())
                .flatMap(customer -> validateBusinessRules(customer.type(), e))
                .flatMap(repository::save)
                .doOnSuccess(p -> log.info("[Service] Active product created successfully, id={}", p.getId()))
                .onErrorResume(err -> {
                    log.error("[Service] Error in create, customerId={}, productType={}, error={}",
                            e.getCustomerId(), e.getProductType(), err.getMessage());
                    return Mono.error(err);
                });
    }

    // =====================================================
    // UPDATE PRODUCT
    // =====================================================
    @Override
    @Transactional
    public Mono<ActiveProductEntity> update(String id, ActiveProductEntity e) {
        log.info("[Service] Updating active product id={}", id);

        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("Active product not found")))
                .flatMap(existing -> {
                    log.info("[Service] Found product id={}, customerId={}, productType={}",
                            id, existing.getCustomerId(), existing.getProductType());

                    existing.setCreditLimit(e.getCreditLimit());
                    existing.setBalance(e.getBalance());

                    log.info("[Service] Validating business rules for update id={}", id);

                    return validateBusinessRules(existing.getCustomerId(), existing)
                            .doOnSuccess(v ->
                                    log.info("[Service] Business rules OK for update id={}", id)
                            )
                            .doOnError(err ->
                                    log.error("[Service] Business rules FAILED for update id={}, error={}",
                                            id, err.getMessage())
                            )
                            // ✅ El flujo continúa SOLO si pasó validateBusinessRules
                            .then(repository.save(existing));
                })
                .doOnSuccess(saved ->
                        log.info("[Service] Active product updated successfully id={}", saved.getId())
                )
                .doOnError(err ->
                        log.error("[Service] Error while updating id={}, error={}", id, err.getMessage())
                );
    }

    // =====================================================
    // SOFT DELETE
    // =====================================================
    @Override
    public Mono<Void> delete(String id) {
        log.info("[Service] Deleting active product id={}, traceId={}", id);
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("Active product not found")))
                .flatMap(existing -> {
                    existing.setActive(false);
                    return repository.save(existing);
                })
                .doOnSuccess(p -> log.info("[Service] Active product deleted (soft) successfully, id={}", p.getId()))
                .onErrorResume(err -> {
                    log.error("[Service] Error in delete, id={}, error={}", id, err.getMessage());
                    return Mono.error(err);
                })
                .then();
    }

    @Override
    public Mono<Double> getBalance(String id) {
        log.info("[Service] Getting balance for active product id={}, traceId={}", id);
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("Active product not found")))
                .map(ActiveProductEntity::getBalance)
                .doOnSuccess(balance -> log.info("[Service] Retrieved balance for active product id={}: {}", id, balance))
                .onErrorResume(err -> {
                    log.error("[Service] Error in getBalance, id={}, error={}", id, err.getMessage());
                    return Mono.error(err);
                });
    }

    @Override
    public Flux<ActiveProductEntity> findByCustomerId(String customerId, @Nullable String productId) {

        log.info("[Service] Finding active products for customerId={}, productId={}, traceId={}", customerId, productId);
        return validateCustomerExists(customerId)
                .flatMapMany(customer -> {
                    if (productId != null) {
                        return repository.findByCustomerIdAndActiveTrue(customerId)
                                .filter(p -> p.getId().equals(productId))
                                .doOnNext(product -> log.info("[Service] Found active product for customer {}: {}", customerId, product.getId()))
                                .doOnError(err -> log.error("[Service] Error fetching active product for customer {}, productId={}, error={}", customerId, productId, err.getMessage()));
                    } else {
                        return repository.findByCustomerIdAndActiveTrue(customerId)
                                .doOnNext(product -> log.info("[Service] Found active product for customer {}: {}", customerId, product.getId()))
                                .doOnError(err -> log.error("[Service] Error fetching active products for customer {}, error={}", customerId, err.getMessage()));
                    }
                })
                .onErrorResume(err -> {
                    log.error("[Service] Error in findByCustomerId, customerId={}, productId={}, error={}", customerId, productId, err.getMessage());
                    return Flux.error(err);
                });
    }

    // ===============================================
    //  BUSNISES RULES
    // ===============================================

    // ===============================================
    // 1. Validar existencia del cliente
    // ===============================================
    private Mono<Customer> validateCustomerExists(String customerId) {
        return customerWebClient.getCustomer(customerId)
                .switchIfEmpty(Mono.error(new BusinessException("Customer not found")));
    }


    // ===============================================
    // 2. Coordinador de reglas (según tipo de cliente)
    // ===============================================
    private Mono<ActiveProductEntity> validateBusinessRules(String customerType,
                                                            ActiveProductEntity product) {

        log.info("[ActiveService] Validating business rules for customerType={} productType={}",
                customerType, product.getProductType());

        if (PERSONAL.equalsIgnoreCase(customerType)) {
            return validatePersonalCustomerRules(product);
        }

        if (BUSINESS.equalsIgnoreCase(customerType)) {
            return validateBusinessCustomerRules(product);
        }

        return Mono.error(new BusinessException("Invalid customerType"));
    }


    // =======================================================
    // 3. Reglas para clientes de tipo PERSONAL
    // =======================================================
    private Mono<ActiveProductEntity> validatePersonalCustomerRules(ActiveProductEntity product) {

        String type = product.getProductType();

        switch (type) {

            case PERSONAL_CREDIT:
                // Máximo un crédito personal por cliente personal
                return validatePersonalCreditLimit(product);

            case CREDIT_CARD:
                // Validación de límite requerida
                return validateCreditCardLimit(product);

            default:
                return Mono.error(new BusinessException("Invalid productType"));
        }
    }


    // =======================================================
    // 4. Reglas para clientes tipo BUSINESS
    // =======================================================
    private Mono<ActiveProductEntity> validateBusinessCustomerRules(ActiveProductEntity product) {

        String type = product.getProductType();

        switch (type) {
            case BUSINESS_CREDIT:
                return Mono.just(product);

            case CREDIT_CARD:
                return validateCreditCardLimit(product);

            default:
                return Mono.error(new BusinessException("Invalid productType"));
        }
    }


    // =======================================================
    // 5. Validación individual: Tarjeta de crédito
    // =======================================================
    private Mono<ActiveProductEntity> validateCreditCardLimit(ActiveProductEntity product) {

        if (product.getCreditLimit() == null || product.getCreditLimit() <= 0) {
            return Mono.error(new BusinessException("Credit cards require a positive creditLimit"));
        }

        repository.findByCustomerIdAndActiveTrue(product.getCustomerId())
                .filter(p -> CREDIT_CARD.equalsIgnoreCase(p.getProductType()))
                .hasElements()
                .flatMap(hasCreditCard -> {
                    if (hasCreditCard) {
                        return Mono.error(new BusinessException(
                                "Customers can only have one CREDIT_CARD product"));
                    }
                    return Mono.just(product);
                });

        return Mono.just(product);
    }


    // =======================================================
    // 6. Validación individual: Crédito PERSONAL (solo 1)
    // =======================================================
    private Mono<ActiveProductEntity> validatePersonalCreditLimit(ActiveProductEntity product) {

        return repository.findByCustomerIdAndActiveTrue(product.getCustomerId())
                .filter(p -> PERSONAL_CREDIT.equalsIgnoreCase(p.getProductType()))
                .hasElements()
                .flatMap(hasPersonalCredit -> {

                    if (hasPersonalCredit) {
                        return Mono.error(new BusinessException(
                                "Personal customers can only have one PERSONAL_CREDIT product"));
                    }

                    return Mono.just(product);
                });
    }


}
