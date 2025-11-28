package com.munitax.ledger.model;

import com.munitax.ledger.enums.EntryStatus;
import com.munitax.ledger.enums.SourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID entryId;
    
    @Column(nullable = false, unique = true)
    private String entryNumber;
    
    @Column(nullable = false)
    private LocalDate entryDate;
    
    @Column(nullable = false)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;
    
    private UUID sourceId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryStatus status;
    
    @Column(nullable = false)
    private UUID tenantId;
    
    private UUID entityId; // Filer or Municipality ID
    
    @Column(nullable = false)
    private UUID createdBy;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private UUID postedBy;
    
    private LocalDateTime postedAt;
    
    private UUID reversedBy;
    
    private LocalDateTime reversedAt;
    
    private UUID reversalEntryId;
    
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JournalEntryLine> lines = new ArrayList<>();
    
    @Transient
    public BigDecimal getTotalDebits() {
        return lines == null ? BigDecimal.ZERO : lines.stream()
                .map(JournalEntryLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Transient
    public BigDecimal getTotalCredits() {
        return lines == null ? BigDecimal.ZERO : lines.stream()
                .map(JournalEntryLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Transient
    public boolean isBalanced() {
        return getTotalDebits().compareTo(getTotalCredits()) == 0;
    }
}
