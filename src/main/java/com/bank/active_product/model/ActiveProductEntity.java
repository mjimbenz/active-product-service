package com.bank.active_product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "active_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveProductEntity {

    @Id
    private String id;

    private String customerId;
    private String productType;
    private Double creditLimit;
    private Double balance;

    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
