package com.bank.active_product.model;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "active_products")
public class ActiveProductEntity {

    @Id
    private String id;

    @NotBlank(message = "El customerId es obligatorio")
    @Indexed
    private String customerId;

    @NotBlank(message = "El tipo de producto es obligatorio")
    @Pattern(
            regexp = "BUSINESS_CREDIT|CREDIT_CARD|CREDIT_CARD|PERSONAL_CREDIT",
            message = "El tipo de producto debe ser PERSONAL_CREDIT o CREDIT_CARD"
    )
    private String productType;

    @NotNull(message = "El límite de crédito no puede ser nulo")
    @Min(value = 0, message = "El límite de crédito no puede ser negativo")
    private Double creditLimit;

    @NotNull(message = "El balance no puede ser nulo")
    @Min(value = 0, message = "El balance no puede ser negativo")
    private Double balance;

    private boolean active;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}