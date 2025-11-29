package com.munitax.ledger.model;

import com.munitax.ledger.enums.AccountType;
import com.munitax.ledger.enums.NormalBalance;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "chart_of_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartOfAccounts {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID accountId;
    
    @Column(nullable = false, unique = true)
    private String accountNumber;
    
    @Column(nullable = false)
    private String accountName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NormalBalance normalBalance;
    
    private UUID parentAccountId;
    
    @Column(nullable = false)
    private UUID tenantId;
    
    private String description;
    
    @Column(nullable = false)
    private Boolean active = true;
}
