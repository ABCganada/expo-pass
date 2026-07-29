package com.coderhan.lastmission.event.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "quantity_total", nullable = false)
    private int quantityTotal;

    @Column(name = "quantity_remaining", nullable = false)
    private int quantityRemaining;

    @Column(name = "max_purchase_per_user", nullable = false)
    private int maxPurchasePerUser;

    @Column(name = "sale_start_at", nullable = false)
    private Instant saleStartAt;

    @Column(name = "sale_end_at", nullable = false)
    private Instant saleEndAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Ticket(Event event, String name, int price, int quantityTotal,
                   int maxPurchasePerUser, Instant saleStartAt, Instant saleEndAt) {
        this.event = event;
        this.name = name;
        this.price = price;
        this.quantityTotal = quantityTotal;
        this.quantityRemaining = quantityTotal;
        this.maxPurchasePerUser = maxPurchasePerUser;
        this.saleStartAt = saleStartAt;
        this.saleEndAt = saleEndAt;
    }
    
    public void updateDetails(String name, int price, int maxPurchasePerUser, Instant saleStartAt, Instant saleEndAt) {
        this.name = name;
        this.price = price;
        this.maxPurchasePerUser = maxPurchasePerUser;
        this.saleStartAt = saleStartAt;
        this.saleEndAt = saleEndAt;
    }
}