package com.rinha2025.__Imp_rinha2025.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String correlationId;
    private Double amount;
    private LocalDateTime createdAt;
    private Boolean isDefault;

    public PaymentEntity() {
    }

    public PaymentEntity(String correlationId, Double amount, LocalDateTime createdAt, Boolean isDefault) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.createdAt = createdAt;
        this.isDefault = isDefault;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

}
