package com.munitax.ledger.controller;

import com.munitax.ledger.dto.JournalEntryRequest;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.service.JournalEntryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journal-entries")
@RequiredArgsConstructor
@Slf4j
public class JournalEntryController {
    
    private final JournalEntryService journalEntryService;
    
    @PostMapping
    public ResponseEntity<JournalEntry> createJournalEntry(@RequestBody JournalEntryRequest request) {
        log.info("Creating journal entry for entity {}", request.getEntityId());
        JournalEntry entry = journalEntryService.createJournalEntry(request);
        return ResponseEntity.ok(entry);
    }
    
    @PostMapping("/{entryId}/reverse")
    public ResponseEntity<JournalEntry> reverseEntry(
            @PathVariable UUID entryId,
            @RequestParam UUID userId,
            @RequestParam String reason) {
        log.info("Reversing journal entry {}", entryId);
        JournalEntry reversingEntry = journalEntryService.reverseEntry(entryId, userId, reason);
        return ResponseEntity.ok(reversingEntry);
    }
    
    @GetMapping("/entity/{tenantId}/{entityId}")
    public ResponseEntity<List<JournalEntry>> getEntriesForEntity(
            @PathVariable UUID tenantId,
            @PathVariable UUID entityId) {
        List<JournalEntry> entries = journalEntryService.getEntriesForEntity(tenantId, entityId);
        return ResponseEntity.ok(entries);
    }
    
    @GetMapping("/{entryId}")
    public ResponseEntity<JournalEntry> getEntry(@PathVariable UUID entryId) {
        JournalEntry entry = journalEntryService.getEntryById(entryId);
        return ResponseEntity.ok(entry);
    }
}
