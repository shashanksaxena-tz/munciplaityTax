package com.munitax.ledger.dto;

import com.munitax.ledger.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryRequest {
    private LocalDate entryDate;
    private String description;
    private SourceType sourceType;
    private UUID sourceId;
    private UUID tenantId;
    private UUID entityId;
    private UUID createdBy;
    private List<JournalEntryLineRequest> lines;
}
