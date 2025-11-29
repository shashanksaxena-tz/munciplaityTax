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
            @PathVariable UUID tenantId,
            @PathVariable UUID filerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Generating statement for filer {}", filerId);
        AccountStatementResponse statement = accountStatementService.generateFilerStatement(
                tenantId, filerId, startDate, endDate);
        return ResponseEntity.ok(statement);
    }
}
