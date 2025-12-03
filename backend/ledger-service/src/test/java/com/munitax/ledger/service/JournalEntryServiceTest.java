package com.munitax.ledger.service;

import com.munitax.ledger.dto.JournalEntryLineRequest;
import com.munitax.ledger.dto.JournalEntryRequest;
import com.munitax.ledger.enums.SourceType;
import com.munitax.ledger.model.ChartOfAccounts;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.repository.ChartOfAccountsRepository;
import com.munitax.ledger.repository.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {
    
    @Mock
    private JournalEntryRepository journalEntryRepository;
    
    @Mock
    private ChartOfAccountsRepository chartOfAccountsRepository;
    
    @Mock
    private AuditLogService auditLogService;
    
    @InjectMocks
    private JournalEntryService journalEntryService;
    
    private ChartOfAccounts cashAccount;
    private ChartOfAccounts taxLiabilityAccount;
    
    @BeforeEach
    void setUp() {
        cashAccount = new ChartOfAccounts();
        cashAccount.setAccountId(UUID.randomUUID());
        cashAccount.setAccountNumber("1000");
        cashAccount.setAccountName("Cash");
        
        taxLiabilityAccount = new ChartOfAccounts();
        taxLiabilityAccount.setAccountId(UUID.randomUUID());
        taxLiabilityAccount.setAccountNumber("2100");
        taxLiabilityAccount.setAccountName("Tax Liability");
    }
    
    @Test
    void testCreateBalancedJournalEntry() {
        // Prepare request
        List<JournalEntryLineRequest> lines = new ArrayList<>();
        lines.add(JournalEntryLineRequest.builder()
                .accountNumber("2100")
                .debit(new BigDecimal("1000.00"))
                .credit(BigDecimal.ZERO)
                .description("Tax liability")
                .build());
        lines.add(JournalEntryLineRequest.builder()
                .accountNumber("1000")
                .debit(BigDecimal.ZERO)
                .credit(new BigDecimal("1000.00"))
                .description("Cash payment")
                .build());
        
        JournalEntryRequest request = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description("Payment")
                .sourceType(SourceType.PAYMENT)
                .sourceId(UUID.randomUUID())
                .tenantId(UUID.randomUUID().toString())
                .entityId(UUID.randomUUID().toString())
                .createdBy(UUID.randomUUID())
                .lines(lines)
                .build();
        
        // Mock repositories
        when(chartOfAccountsRepository.findByAccountNumber("2100"))
                .thenReturn(Optional.of(taxLiabilityAccount));
        when(chartOfAccountsRepository.findByAccountNumber("1000"))
                .thenReturn(Optional.of(cashAccount));
        when(journalEntryRepository.save(any(JournalEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(journalEntryRepository.findMaxEntryNumberByPrefix(any(), any()))
                .thenReturn(null);
        
        // Execute
        JournalEntry result = journalEntryService.createJournalEntry(request);
        
        // Verify
        assertNotNull(result);
        assertTrue(result.isBalanced());
        assertEquals(new BigDecimal("1000.00"), result.getTotalDebits());
        assertEquals(new BigDecimal("1000.00"), result.getTotalCredits());
        verify(journalEntryRepository, times(1)).save(any(JournalEntry.class));
        verify(auditLogService, times(1)).logAction(any(), any(), any(), any(), any(), any());
    }
    
    @Test
    void testCreateUnbalancedJournalEntryShouldFail() {
        // Prepare unbalanced request
        List<JournalEntryLineRequest> lines = new ArrayList<>();
        lines.add(JournalEntryLineRequest.builder()
                .accountNumber("2100")
                .debit(new BigDecimal("1000.00"))
                .credit(BigDecimal.ZERO)
                .build());
        lines.add(JournalEntryLineRequest.builder()
                .accountNumber("1000")
                .debit(BigDecimal.ZERO)
                .credit(new BigDecimal("500.00")) // Unbalanced!
                .build());
        
        JournalEntryRequest request = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description("Unbalanced entry")
                .sourceType(SourceType.PAYMENT)
                .tenantId(UUID.randomUUID().toString())
                .entityId(UUID.randomUUID().toString())
                .createdBy(UUID.randomUUID())
                .lines(lines)
                .build();
        
        // Execute and verify exception
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> journalEntryService.createJournalEntry(request)
        );
        
        assertTrue(exception.getMessage().contains("not balanced"));
        verify(journalEntryRepository, never()).save(any());
    }
}
