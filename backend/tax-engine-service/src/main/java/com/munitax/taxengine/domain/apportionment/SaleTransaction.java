package com.munitax.taxengine.domain.apportionment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Sale Transaction entity representing individual sales for detailed sourcing tracking.
 * Each transaction has a sourcing method and state allocation.
 */
@Entity
@Table(name = "sale_transaction", indexes = {
    @Index(name = "idx_sale_transaction_sales_factor_id", columnList = "sales_factor_id"),
    @Index(name = "idx_sale_transaction_date", columnList = "transaction_date"),
    @Index(name = "idx_sale_transaction_allocated_state", columnList = "allocated_state")
})
@Data
@NoArgsConstructor
public class SaleTransaction {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    @ManyToOne
    @JoinColumn(name = "sales_factor_id", nullable = false)
    private SalesFactor salesFactor;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "sale_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal saleAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 30)
    private SaleType saleType;

    @Column(name = "origin_state", length = 2)
    private String originState;

    @Column(name = "destination_state", length = 2)
    private String destinationState;

    /**
     * Customer state for market-based service sourcing.
     * T097 [US3]: Added for service revenue sourcing.
     */
    @Column(name = "customer_state", length = 2)
    private String customerState;

    /**
     * Service sourcing method used (market-based or cost-of-performance).
     * T097 [US3]: Added for service revenue tracking.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_sourcing_method", length = 30)
    private ServiceSourcingMethod serviceSourcingMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "sourcing_method", nullable = false, length = 30)
    private SourcingMethod sourcingMethod;

    @Column(name = "has_destination_nexus")
    private Boolean hasDestinationNexus = false;

    @Column(name = "allocated_state", nullable = false, length = 2)
    private String allocatedState;

    @Column(name = "allocated_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedAmount;

    @Type(JsonType.class)
    @Column(name = "customer_location_details", columnDefinition = "jsonb")
    private Map<String, Object> customerLocationDetails;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "throwback_applied")
    private Boolean throwbackApplied = false;

    @Column(name = "throwback_amount", precision = 15, scale = 2)
    private BigDecimal throwbackAmount = BigDecimal.ZERO;

    @Column(name = "ohio_sourced_amount", precision = 15, scale = 2)
    private BigDecimal ohioSourcedAmount = BigDecimal.ZERO;

    @Column(name = "schedule_y_id")
    private UUID scheduleYId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    /**
     * Get the sale amount (convenience method).
     */
    public BigDecimal getAmount() {
        return saleAmount;
    }

    /**
     * Constructor for simple destination-based transaction.
     */
    public SaleTransaction(LocalDate transactionDate, String customerName, BigDecimal saleAmount, 
                          SaleType saleType, String originState, String destinationState) {
        this.transactionDate = transactionDate;
        this.customerName = customerName;
        this.saleAmount = saleAmount;
        this.saleType = saleType;
        this.originState = originState;
        this.destinationState = destinationState;
        this.allocatedAmount = saleAmount;
    }
}
