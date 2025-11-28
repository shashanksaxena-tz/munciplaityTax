package com.munitax.taxengine.domain.apportionment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sales Factor entity for apportionment calculation.
 * Formula: (Ohio sales) / (Total sales everywhere) × 100%
 */
@Entity
@Table(name = "sales_factor")
@Data
@NoArgsConstructor
public class SalesFactor {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "sales_factor_id", updatable = false, nullable = false)
    private UUID salesFactorId;

    @OneToOne
    @JoinColumn(name = "schedule_y_id", nullable = false)
    private ScheduleY scheduleY;

    @Column(name = "ohio_sales_tangible_goods", precision = 15, scale = 2)
    private BigDecimal ohioSalesTangibleGoods = BigDecimal.ZERO;

    @Column(name = "ohio_sales_services", precision = 15, scale = 2)
    private BigDecimal ohioSalesServices = BigDecimal.ZERO;

    @Column(name = "ohio_sales_rental_income", precision = 15, scale = 2)
    private BigDecimal ohioSalesRentalIncome = BigDecimal.ZERO;

    @Column(name = "ohio_sales_interest", precision = 15, scale = 2)
    private BigDecimal ohioSalesInterest = BigDecimal.ZERO;

    @Column(name = "ohio_sales_royalties", precision = 15, scale = 2)
    private BigDecimal ohioSalesRoyalties = BigDecimal.ZERO;

    @Column(name = "ohio_sales_other", precision = 15, scale = 2)
    private BigDecimal ohioSalesOther = BigDecimal.ZERO;

    @Column(name = "throwback_adjustment", precision = 15, scale = 2)
    private BigDecimal throwbackAdjustment = BigDecimal.ZERO;

    @Column(name = "total_ohio_sales", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalOhioSales;

    @Column(name = "total_sales_everywhere", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSalesEverywhere;

    @Column(name = "sales_factor_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal salesFactorPercentage;

    @OneToMany(mappedBy = "salesFactor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleTransaction> transactions = new ArrayList<>();

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        lastModifiedDate = LocalDateTime.now();
        calculateTotalsAndPercentage();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        calculateTotalsAndPercentage();
    }

    /**
     * Calculate total Ohio sales and sales factor percentage.
     */
    public void calculateTotalsAndPercentage() {
        // Calculate total Ohio sales
        this.totalOhioSales = (ohioSalesTangibleGoods != null ? ohioSalesTangibleGoods : BigDecimal.ZERO)
            .add(ohioSalesServices != null ? ohioSalesServices : BigDecimal.ZERO)
            .add(ohioSalesRentalIncome != null ? ohioSalesRentalIncome : BigDecimal.ZERO)
            .add(ohioSalesInterest != null ? ohioSalesInterest : BigDecimal.ZERO)
            .add(ohioSalesRoyalties != null ? ohioSalesRoyalties : BigDecimal.ZERO)
            .add(ohioSalesOther != null ? ohioSalesOther : BigDecimal.ZERO)
            .add(throwbackAdjustment != null ? throwbackAdjustment : BigDecimal.ZERO);

        // Calculate sales factor percentage
        if (totalSalesEverywhere != null && totalSalesEverywhere.compareTo(BigDecimal.ZERO) > 0) {
            this.salesFactorPercentage = totalOhioSales
                .divide(totalSalesEverywhere, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            this.salesFactorPercentage = BigDecimal.ZERO;
        }
    }

    /**
     * Add transaction to this sales factor.
     */
    public void addTransaction(SaleTransaction transaction) {
        transactions.add(transaction);
        transaction.setSalesFactor(this);
    }

    /**
     * Remove transaction from this sales factor.
     */
    public void removeTransaction(SaleTransaction transaction) {
        transactions.remove(transaction);
        transaction.setSalesFactor(null);
    }
}
