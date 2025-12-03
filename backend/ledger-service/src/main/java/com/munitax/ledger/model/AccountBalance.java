package com.munitax.ledger.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "account_balances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID balanceId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccounts account;
    
    @Column(nullable = false)
    private UUID entityId; // Filer or Municipality ID
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column(nullable = false)
    private LocalDate periodStartDate;
    
    @Column(nullable = false)
    private LocalDate periodEndDate;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginningBalance;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDebits;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCredits;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal endingBalance;
}
