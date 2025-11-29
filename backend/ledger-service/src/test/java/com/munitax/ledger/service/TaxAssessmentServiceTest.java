package com.munitax.ledger.service;

import com.munitax.ledger.dto.JournalEntryLineRequest;
import com.munitax.ledger.dto.JournalEntryRequest;
import com.munitax.ledger.enums.SourceType;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.model.JournalEntryLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaxAssessmentService Tests")
class TaxAssessmentServiceTest {

    @Mock
    private JournalEntryService journalEntryService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TaxAssessmentService taxAssessmentService;

    private UUID tenantId;
    private UUID filerId;
    private UUID returnId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        filerId = UUID.randomUUID();
        returnId = UUID.randomUUID();
    }

    @Test
    @DisplayName("T011 - Should create journal entries for simple tax assessment")
    void shouldCreateJournalEntriesForSimpleTaxAssessment() {
        // Given
        BigDecimal taxAmount = new BigDecimal("10000.00");
        BigDecimal penaltyAmount = BigDecimal.ZERO;
        BigDecimal interestAmount = BigDecimal.ZERO;
        String taxYear = "2024";
        String taxPeriod = "Q1";

        JournalEntry mockFilerEntry = createMockJournalEntry(filerId, returnId, taxAmount);
        JournalEntry mockMunicipalityEntry = createMockJournalEntry(UUID.randomUUID(), returnId, taxAmount);

        when(journalEntryService.createJournalEntry(any(JournalEntryRequest.class)))
                .thenReturn(mockFilerEntry)
                .thenReturn(mockMunicipalityEntry);

        // When
        JournalEntry result = taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, returnId, taxAmount, penaltyAmount, interestAmount, taxYear, taxPeriod
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntityId()).isEqualTo(filerId);
        assertThat(result.getSourceType()).isEqualTo(SourceType.TAX_ASSESSMENT);

        // Verify two journal entries created (filer and municipality)
        ArgumentCaptor<JournalEntryRequest> captor = ArgumentCaptor.forClass(JournalEntryRequest.class);
        verify(journalEntryService, times(2)).createJournalEntry(captor.capture());

        List<JournalEntryRequest> capturedRequests = captor.getAllValues();
        
        // Verify filer entry
        JournalEntryRequest filerRequest = capturedRequests.get(0);
        assertThat(filerRequest.getEntityId()).isEqualTo(filerId);
        assertThat(filerRequest.getSourceType()).isEqualTo(SourceType.TAX_ASSESSMENT);
        assertThat(filerRequest.getSourceId()).isEqualTo(returnId);
        assertThat(filerRequest.getLines()).hasSize(2); // Tax liability (credit) + Tax expense (debit)

        // Verify filer journal entry lines balance
        BigDecimal filerDebits = filerRequest.getLines().stream()
                .map(JournalEntryLineRequest::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal filerCredits = filerRequest.getLines().stream()
                .map(JournalEntryLineRequest::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(filerDebits).isEqualByComparingTo(filerCredits);
        assertThat(filerDebits).isEqualByComparingTo(taxAmount);

        // Verify municipality entry
        JournalEntryRequest municipalityRequest = capturedRequests.get(1);
        assertThat(municipalityRequest.getSourceType()).isEqualTo(SourceType.TAX_ASSESSMENT);
        assertThat(municipalityRequest.getSourceId()).isEqualTo(returnId);
        assertThat(municipalityRequest.getLines()).hasSize(2); // AR (debit) + Revenue (credit)

        // Verify municipality journal entry lines balance
        BigDecimal municipalityDebits = municipalityRequest.getLines().stream()
                .map(JournalEntryLineRequest::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal municipalityCredits = municipalityRequest.getLines().stream()
                .map(JournalEntryLineRequest::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(municipalityDebits).isEqualByComparingTo(municipalityCredits);
        assertThat(municipalityDebits).isEqualByComparingTo(taxAmount);

        // Verify audit log
        verify(auditLogService).logAction(
                eq(mockFilerEntry.getEntryId()),
                eq("TAX_ASSESSMENT"),
                eq("CREATE"),
                eq(filerId),
                eq(tenantId),
                anyString()
        );
    }

    @Test
    @DisplayName("T012 - Should create journal entries for compound tax assessment (tax + penalty + interest)")
    void shouldCreateJournalEntriesForCompoundTaxAssessment() {
        // Given
        BigDecimal taxAmount = new BigDecimal("10000.00");
        BigDecimal penaltyAmount = new BigDecimal("500.00");
        BigDecimal interestAmount = new BigDecimal("150.00");
        BigDecimal totalAmount = new BigDecimal("10650.00");
        String taxYear = "2024";
        String taxPeriod = "Q1";

        JournalEntry mockFilerEntry = createMockJournalEntry(filerId, returnId, totalAmount);
        JournalEntry mockMunicipalityEntry = createMockJournalEntry(UUID.randomUUID(), returnId, totalAmount);

        when(journalEntryService.createJournalEntry(any(JournalEntryRequest.class)))
                .thenReturn(mockFilerEntry)
                .thenReturn(mockMunicipalityEntry);

        // When
        JournalEntry result = taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, returnId, taxAmount, penaltyAmount, interestAmount, taxYear, taxPeriod
        );

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<JournalEntryRequest> captor = ArgumentCaptor.forClass(JournalEntryRequest.class);
        verify(journalEntryService, times(2)).createJournalEntry(captor.capture());

        List<JournalEntryRequest> capturedRequests = captor.getAllValues();
        
        // Verify filer entry has lines for tax, penalty, and interest
        JournalEntryRequest filerRequest = capturedRequests.get(0);
        assertThat(filerRequest.getLines()).hasSize(6); // 3 liability credits + 3 expense debits

        // Check that filer has tax liability (2100), penalty liability (2120), and interest liability (2130)
        List<String> filerAccountNumbers = filerRequest.getLines().stream()
                .map(JournalEntryLineRequest::getAccountNumber)
                .toList();
        assertThat(filerAccountNumbers).contains("2100", "2120", "2130", "6100");

        // Verify filer journal entry balances
        BigDecimal filerDebits = filerRequest.getLines().stream()
                .map(JournalEntryLineRequest::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal filerCredits = filerRequest.getLines().stream()
                .map(JournalEntryLineRequest::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(filerDebits).isEqualByComparingTo(filerCredits);
        assertThat(filerDebits).isEqualByComparingTo(totalAmount);

        // Verify municipality entry has lines for tax, penalty, and interest revenue
        JournalEntryRequest municipalityRequest = capturedRequests.get(1);
        assertThat(municipalityRequest.getLines()).hasSize(6); // 3 AR debits + 3 revenue credits

        // Check that municipality has AR (1201) and revenue accounts (4100, 4200, 4300)
        List<String> municipalityAccountNumbers = municipalityRequest.getLines().stream()
                .map(JournalEntryLineRequest::getAccountNumber)
                .toList();
        assertThat(municipalityAccountNumbers).contains("1201", "4100", "4200", "4300");

        // Verify municipality journal entry balances
        BigDecimal municipalityDebits = municipalityRequest.getLines().stream()
                .map(JournalEntryLineRequest::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal municipalityCredits = municipalityRequest.getLines().stream()
                .map(JournalEntryLineRequest::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(municipalityDebits).isEqualByComparingTo(municipalityCredits);
        assertThat(municipalityDebits).isEqualByComparingTo(totalAmount);

        // Verify audit log includes all amounts
        verify(auditLogService).logAction(
                eq(mockFilerEntry.getEntryId()),
                eq("TAX_ASSESSMENT"),
                eq("CREATE"),
                eq(filerId),
                eq(tenantId),
                contains("10650.00")
        );
    }

    @Test
    @DisplayName("T012 - Should handle tax assessment with only penalty")
    void shouldHandleTaxAssessmentWithOnlyPenalty() {
        // Given
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal penaltyAmount = new BigDecimal("500.00");
        BigDecimal interestAmount = BigDecimal.ZERO;
        String taxYear = "2024";
        String taxPeriod = "Q1";

        JournalEntry mockFilerEntry = createMockJournalEntry(filerId, returnId, penaltyAmount);
        JournalEntry mockMunicipalityEntry = createMockJournalEntry(UUID.randomUUID(), returnId, penaltyAmount);

        when(journalEntryService.createJournalEntry(any(JournalEntryRequest.class)))
                .thenReturn(mockFilerEntry)
                .thenReturn(mockMunicipalityEntry);

        // When
        JournalEntry result = taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, returnId, taxAmount, penaltyAmount, interestAmount, taxYear, taxPeriod
        );

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<JournalEntryRequest> captor = ArgumentCaptor.forClass(JournalEntryRequest.class);
        verify(journalEntryService, times(2)).createJournalEntry(captor.capture());

        List<JournalEntryRequest> capturedRequests = captor.getAllValues();
        
        // Verify filer entry has only penalty lines
        JournalEntryRequest filerRequest = capturedRequests.get(0);
        assertThat(filerRequest.getLines()).hasSize(2); // Penalty liability + Penalty expense

        // Verify amounts balance
        BigDecimal filerDebits = filerRequest.getLines().stream()
                .map(JournalEntryLineRequest::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal filerCredits = filerRequest.getLines().stream()
                .map(JournalEntryLineRequest::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(filerDebits).isEqualByComparingTo(penaltyAmount);
        assertThat(filerCredits).isEqualByComparingTo(penaltyAmount);
    }

    @Test
    @DisplayName("T012 - Should handle tax assessment with only interest")
    void shouldHandleTaxAssessmentWithOnlyInterest() {
        // Given
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal penaltyAmount = BigDecimal.ZERO;
        BigDecimal interestAmount = new BigDecimal("150.00");
        String taxYear = "2024";
        String taxPeriod = "Q1";

        JournalEntry mockFilerEntry = createMockJournalEntry(filerId, returnId, interestAmount);
        JournalEntry mockMunicipalityEntry = createMockJournalEntry(UUID.randomUUID(), returnId, interestAmount);

        when(journalEntryService.createJournalEntry(any(JournalEntryRequest.class)))
                .thenReturn(mockFilerEntry)
                .thenReturn(mockMunicipalityEntry);

        // When
        JournalEntry result = taxAssessmentService.recordTaxAssessment(
                tenantId, filerId, returnId, taxAmount, penaltyAmount, interestAmount, taxYear, taxPeriod
        );

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<JournalEntryRequest> captor = ArgumentCaptor.forClass(JournalEntryRequest.class);
        verify(journalEntryService, times(2)).createJournalEntry(captor.capture());

        List<JournalEntryRequest> capturedRequests = captor.getAllValues();
        
        // Verify filer entry has only interest lines
        JournalEntryRequest filerRequest = capturedRequests.get(0);
        assertThat(filerRequest.getLines()).hasSize(2); // Interest liability + Interest expense

        // Verify amounts balance
        BigDecimal filerDebits = filerRequest.getLines().stream()
                .map(JournalEntryLineRequest::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal filerCredits = filerRequest.getLines().stream()
                .map(JournalEntryLineRequest::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(filerDebits).isEqualByComparingTo(interestAmount);
        assertThat(filerCredits).isEqualByComparingTo(interestAmount);
    }

    private JournalEntry createMockJournalEntry(UUID entityId, UUID sourceId, BigDecimal amount) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryId(UUID.randomUUID());
        entry.setEntityId(entityId);
        entry.setSourceType(SourceType.TAX_ASSESSMENT);
        entry.setSourceId(sourceId);
        entry.setEntryDate(LocalDate.now());
        entry.setDescription("Test tax assessment");
        
        List<JournalEntryLine> lines = new ArrayList<>();
        JournalEntryLine debitLine = new JournalEntryLine();
        debitLine.setLineId(UUID.randomUUID());
        debitLine.setAccountNumber("1000");
        debitLine.setDebit(amount);
        debitLine.setCredit(BigDecimal.ZERO);
        lines.add(debitLine);

        JournalEntryLine creditLine = new JournalEntryLine();
        creditLine.setLineId(UUID.randomUUID());
        creditLine.setAccountNumber("2000");
        creditLine.setDebit(BigDecimal.ZERO);
        creditLine.setCredit(amount);
        lines.add(creditLine);

        entry.setLines(lines);
        return entry;
    }
}
