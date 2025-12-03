package com.munitax.ledger.dto;

import com.munitax.ledger.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
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
    private String tenantId;
    private String entityId;
    private UUID createdBy;
    @Builder.Default
    private List<JournalEntryLineRequest> lines = new ArrayList<>();
}
