package com.munitax.ledger.controller;

import com.munitax.ledger.dto.AccountStatementResponse;
import com.munitax.ledger.service.AccountStatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/statements")
@RequiredArgsConstructor
@Slf4j
public class AccountStatementController {
    
    private final AccountStatementService accountStatementService;
    
    @GetMapping("/filer/{tenantId}/{filerId}")
    public ResponseEntity<AccountStatementResponse> getFilerStatement(
            @PathVariable String tenantId,
            @PathVariable String filerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Generating statement for filer {}", filerId);
        AccountStatementResponse statement = accountStatementService.generateFilerStatement(
                tenantId, filerId, startDate, endDate);
        return ResponseEntity.ok(statement);
    }
    
    /**
     * T035: Export account statement to PDF format.
     * 
     * @param tenantId The tenant identifier
     * @param filerId The filer identifier
     * @return PDF file as byte array
     */
    @GetMapping("/filer/{tenantId}/{filerId}/pdf")
    public ResponseEntity<byte[]> exportStatementToPdf(
            @PathVariable String tenantId,
            @PathVariable String filerId) {
        
        log.info("Exporting statement to PDF for filer {}", filerId);
        byte[] pdfContent = accountStatementService.exportStatementToPdf(tenantId, filerId);
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=statement-" + filerId + ".pdf")
                .body(pdfContent);
    }
    
    /**
     * T036: Export account statement to CSV format.
     * 
     * @param tenantId The tenant identifier
     * @param filerId The filer identifier
     * @return CSV file as string
     */
    @GetMapping("/filer/{tenantId}/{filerId}/csv")
    public ResponseEntity<String> exportStatementToCsv(
            @PathVariable String tenantId,
            @PathVariable String filerId) {
        
        log.info("Exporting statement to CSV for filer {}", filerId);
        String csvContent = accountStatementService.exportStatementToCsv(tenantId, filerId);
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=statement-" + filerId + ".csv")
                .body(csvContent);
    }
}
