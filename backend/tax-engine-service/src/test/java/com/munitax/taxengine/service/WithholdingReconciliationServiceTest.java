package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.withholding.FilingFrequency;
import com.munitax.taxengine.domain.withholding.W1Filing;
import com.munitax.taxengine.domain.withholding.W1FilingStatus;
import com.munitax.taxengine.dto.ReconciliationIssue;
import com.munitax.taxengine.model.W2Form;
import com.munitax.taxengine.repository.W1FilingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WithholdingReconciliationService.
 * Tests comprehensive reconciliation logic for W-1 filings.
 */
@ExtendWith(MockitoExtension.class)
class WithholdingReconciliationServiceTest {

    @Mock
    private W1FilingRepository w1FilingRepository;

    @InjectMocks
    private WithholdingReconciliationService reconciliationService;

    private UUID testEmployerId;
    private Integer testTaxYear;

    @BeforeEach
    void setUp() {
        testEmployerId = UUID.randomUUID();
        testTaxYear = 2024;
    }

    @Test
    @DisplayName("Should return empty list when no W-1 filings exist")
    void shouldReturnEmptyListWhenNoFilings() {
        // Given
        when(w1FilingRepository.findByBusinessIdAndTaxYear(testEmployerId, testTaxYear))
            .thenReturn(Collections.emptyList());

        // When
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            testEmployerId, testTaxYear, null);

        // Then
        assertThat(issues).isEmpty();
        verify(w1FilingRepository, times(1)).findByBusinessIdAndTaxYear(testEmployerId, testTaxYear);
    }

    @Test
    @DisplayName("Should detect wage mismatch between W-1 and W-2 federal wages")
    void shouldDetectFederalWageMismatch() {
        // Given
        List<W1Filing> w1Filings = createQuarterlyFilings(
            new BigDecimal("100000"), // Q1
            new BigDecimal("100000"), // Q2
            new BigDecimal("100000"), // Q3
            new BigDecimal("100000")  // Q4
        );
        
        List<W2Form> w2Forms = List.of(
            createW2Form(405000.0, 405000.0) // $5,000 variance
        );
        
        when(w1FilingRepository.findByBusinessIdAndTaxYear(testEmployerId, testTaxYear))
            .thenReturn(w1Filings);

        // When
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            testEmployerId, testTaxYear, w2Forms);

        // Then
        assertThat(issues).isNotEmpty();
        Optional<ReconciliationIssue> wageMismatch = issues.stream()
            .filter(i -> i.getIssueType() == ReconciliationIssue.IssueType.WAGE_MISMATCH_FEDERAL)
            .findFirst();
        
        assertThat(wageMismatch).isPresent();
        assertThat(wageMismatch.get().getSeverity()).isEqualTo(ReconciliationIssue.IssueSeverity.HIGH);
        assertThat(wageMismatch.get().getVariance().abs()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should detect wage mismatch between W-1 and W-2 local wages")
    void shouldDetectLocalWageMismatch() {
        // Given
        List<W1Filing> w1Filings = createQuarterlyFilings(
            new BigDecimal("50000"),
            new BigDecimal("50000"),
            new BigDecimal("50000"),
            new BigDecimal("50000")
        );
        
        List<W2Form> w2Forms = List.of(
            createW2Form(200000.0, 203000.0) // $3,000 local wage variance
        );
        
        when(w1FilingRepository.findByBusinessIdAndTaxYear(testEmployerId, testTaxYear))
            .thenReturn(w1Filings);

        // When
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            testEmployerId, testTaxYear, w2Forms);

        // Then
        assertThat(issues).isNotEmpty();
        Optional<ReconciliationIssue> localWageMismatch = issues.stream()
            .filter(i -> i.getIssueType() == ReconciliationIssue.IssueType.WAGE_MISMATCH_LOCAL)
            .findFirst();
        
        assertThat(localWageMismatch).isPresent();
        assertThat(localWageMismatch.get().getSeverity()).isEqualTo(ReconciliationIssue.IssueSeverity.HIGH);
    }

    @Test
    @DisplayName("Should detect invalid withholding rate outside 0-3% range")
    void shouldDetectInvalidWithholdingRate() {
        // Given
        W1Filing filing = createW1Filing("Q1", new BigDecimal("100000"), new BigDecimal("0.0350")); // 3.5%
        
        when(w1FilingRepository.findByBusinessIdAndTaxYear(testEmployerId, testTaxYear))
            .thenReturn(List.of(filing));

        // When
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            testEmployerId, testTaxYear, null);

        // Then
        assertThat(issues).isNotEmpty();
        Optional<ReconciliationIssue> rateIssue = issues.stream()
            .filter(i -> i.getIssueType() == ReconciliationIssue.IssueType.WITHHOLDING_RATE_INVALID)
            .findFirst();
        
        assertThat(rateIssue).isPresent();
        assertThat(rateIssue.get().getSeverity()).isEqualTo(ReconciliationIssue.IssueSeverity.CRITICAL);
    }

    @Test
    @DisplayName("Should detect duplicate filings for same period")
    void shouldDetectDuplicateFilings() {
        // Given - Two non-amended filings for Q1
        W1Filing filing1 = createW1Filing("Q1", new BigDecimal("50000"), new BigDecimal("0.0200"));
        W1Filing filing2 = createW1Filing("Q1", new BigDecimal("50000"), new BigDecimal("0.0200"));
        filing2.setId(UUID.randomUUID()); // Different ID
        
        when(w1FilingRepository.findByBusinessIdAndTaxYear(testEmployerId, testTaxYear))
            .thenReturn(List.of(filing1, filing2));

        // When
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            testEmployerId, testTaxYear, null);

        // Then
        assertThat(issues).isNotEmpty();
        Optional<ReconciliationIssue> duplicateIssue = issues.stream()
            .filter(i -> i.getIssueType() == ReconciliationIssue.IssueType.DUPLICATE_FILING)
            .findFirst();
        
        assertThat(duplicateIssue).isPresent();
        assertThat(duplicateIssue.get().getSeverity()).isEqualTo(ReconciliationIssue.IssueSeverity.HIGH);
        assertThat(duplicateIssue.get().getPeriod()).isEqualTo("Q1");
    }

    @Test
    @DisplayName("Should detect late filings")
    void shouldDetectLateFilings() {
        // Given - Filing due on April 30, filed on May 15 (15 days late)
        W1Filing lateFiling = createW1Filing("Q1", new BigDecimal("100000"), new BigDecimal("0.0200"));
        lateFiling.setDueDate(LocalDate.of(2024, 4, 30));
        lateFiling.setFilingDate(LocalDateTime.of(2024, 5, 15, 10, 0));
        
        when(w1FilingRepository.findByBusinessIdAndTaxYear(testEmployerId, testTaxYear))
            .thenReturn(List.of(lateFiling));

        // When
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            testEmployerId, testTaxYear, null);

        // Then
        assertThat(issues).isNotEmpty();
        Optional<ReconciliationIssue> lateIssue = issues.stream()
            .filter(i -> i.getIssueType() == ReconciliationIssue.IssueType.LATE_FILING)
            .findFirst();
        
        assertThat(lateIssue).isPresent();
        assertThat(lateIssue.get().getSeverity()).isEqualTo(ReconciliationIssue.IssueSeverity.MEDIUM);
        assertThat(lateIssue.get().getDescription()).contains("15 days late");
    }

    @Test
    @DisplayName("Should detect missing quarterly filings")
    void shouldDetectMissingQuarterlyFilings() {
        // Given - Only Q1 and Q2 filed, Q3 and Q4 missing
        List<W1Filing> w1Filings = List.of(
            createW1Filing("Q1", new BigDecimal("50000"), new BigDecimal("0.0200")),
            createW1Filing("Q2", new BigDecimal("50000"), new BigDecimal("0.0200"))
        );
        
        when(w1FilingRepository.findByBusinessIdAndTaxYear(testEmployerId, testTaxYear))
            .thenReturn(w1Filings);

        // When
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            testEmployerId, testTaxYear, null);

        // Then
        assertThat(issues).isNotEmpty();
        List<ReconciliationIssue> missingIssues = issues.stream()
            .filter(i -> i.getIssueType() == ReconciliationIssue.IssueType.MISSING_FILING)
            .toList();
        
        assertThat(missingIssues).hasSize(2); // Q3 and Q4
        assertThat(missingIssues).allMatch(i -> i.getSeverity() == ReconciliationIssue.IssueSeverity.CRITICAL);
    }

    @Test
    @DisplayName("Should detect missing monthly filings")
    void shouldDetectMissingMonthlyFilings() {
        // Given - Only January (M01) filed, 11 months missing
        W1Filing filing = createW1Filing("M01", new BigDecimal("50000"), new BigDecimal("0.0200"));
        filing.setFilingFrequency(FilingFrequency.MONTHLY);
        
        when(w1FilingRepository.findByBusinessIdAndTaxYear(testEmployerId, testTaxYear))
            .thenReturn(List.of(filing));

        // When
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            testEmployerId, testTaxYear, null);

        // Then
        assertThat(issues).isNotEmpty();
        List<ReconciliationIssue> missingIssues = issues.stream()
            .filter(i -> i.getIssueType() == ReconciliationIssue.IssueType.MISSING_FILING)
            .toList();
        
        assertThat(missingIssues).hasSize(11); // M02 through M12
    }

    @Test
    @DisplayName("Should not flag issues when all filings are correct")
    void shouldNotFlagIssuesWhenAllCorrect() {
        // Given - All quarterly filings present, on time, with correct rates
        List<W1Filing> w1Filings = createQuarterlyFilings(
            new BigDecimal("100000"),
            new BigDecimal("100000"),
            new BigDecimal("100000"),
            new BigDecimal("100000")
        );
        
        // Set all filings as on-time
        LocalDate[] dueDates = {
            LocalDate.of(2024, 4, 30),
            LocalDate.of(2024, 7, 30),
            LocalDate.of(2024, 10, 30),
            LocalDate.of(2025, 1, 30)
        };
        
        for (int i = 0; i < w1Filings.size(); i++) {
            w1Filings.get(i).setDueDate(dueDates[i]);
            w1Filings.get(i).setFilingDate(LocalDateTime.of(2024, 3 * (i + 1), 15, 10, 0));
        }
        
        List<W2Form> w2Forms = List.of(
            createW2Form(400000.0, 400000.0) // Matches W-1 total
        );
        
        when(w1FilingRepository.findByBusinessIdAndTaxYear(testEmployerId, testTaxYear))
            .thenReturn(w1Filings);

        // When
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            testEmployerId, testTaxYear, w2Forms);

        // Then - Only cumulative mismatch issues (if any rounding differences)
        List<ReconciliationIssue> criticalIssues = issues.stream()
            .filter(i -> i.getSeverity() == ReconciliationIssue.IssueSeverity.CRITICAL ||
                        i.getSeverity() == ReconciliationIssue.IssueSeverity.HIGH)
            .toList();
        
        assertThat(criticalIssues).isEmpty();
    }

    @Test
    @DisplayName("Should handle W-1 filings without W-2 forms")
    void shouldHandleFilingsWithoutW2Forms() {
        // Given
        List<W1Filing> w1Filings = createQuarterlyFilings(
            new BigDecimal("100000"),
            new BigDecimal("100000"),
            new BigDecimal("100000"),
            new BigDecimal("100000")
        );
        
        when(w1FilingRepository.findByBusinessIdAndTaxYear(testEmployerId, testTaxYear))
            .thenReturn(w1Filings);

        // When
        List<ReconciliationIssue> issues = reconciliationService.reconcileW1Filings(
            testEmployerId, testTaxYear, null);

        // Then - Should not have wage mismatch issues since no W-2 forms provided
        Optional<ReconciliationIssue> wageMismatch = issues.stream()
            .filter(i -> i.getIssueType() == ReconciliationIssue.IssueType.WAGE_MISMATCH_FEDERAL ||
                        i.getIssueType() == ReconciliationIssue.IssueType.WAGE_MISMATCH_LOCAL)
            .findFirst();
        
        assertThat(wageMismatch).isEmpty();
    }

    // Helper methods

    private W1Filing createW1Filing(String period, BigDecimal grossWages, BigDecimal taxRate) {
        BigDecimal taxDue = grossWages.multiply(taxRate).setScale(2, java.math.RoundingMode.HALF_UP);
        
        return W1Filing.builder()
            .id(UUID.randomUUID())
            .tenantId(UUID.randomUUID())
            .businessId(testEmployerId)
            .taxYear(testTaxYear)
            .filingFrequency(FilingFrequency.QUARTERLY)
            .period(period)
            .periodStartDate(LocalDate.of(2024, 1, 1))
            .periodEndDate(LocalDate.of(2024, 3, 31))
            .dueDate(LocalDate.of(2024, 4, 30))
            .filingDate(LocalDateTime.of(2024, 4, 15, 10, 0))
            .grossWages(grossWages)
            .taxableWages(grossWages)
            .taxRate(taxRate)
            .taxDue(taxDue)
            .adjustments(BigDecimal.ZERO)
            .totalAmountDue(taxDue)
            .isAmended(false)
            .status(W1FilingStatus.FILED)
            .lateFilingPenalty(BigDecimal.ZERO)
            .underpaymentPenalty(BigDecimal.ZERO)
            .createdBy(UUID.randomUUID())
            .build();
    }

    private List<W1Filing> createQuarterlyFilings(BigDecimal q1Wages, BigDecimal q2Wages, 
                                                   BigDecimal q3Wages, BigDecimal q4Wages) {
        BigDecimal taxRate = new BigDecimal("0.0200");
        
        return List.of(
            createW1Filing("Q1", q1Wages, taxRate),
            createW1Filing("Q2", q2Wages, taxRate),
            createW1Filing("Q3", q3Wages, taxRate),
            createW1Filing("Q4", q4Wages, taxRate)
        );
    }

    private W2Form createW2Form(Double federalWages, Double localWages) {
        return new W2Form(
            UUID.randomUUID().toString(), // id
            "w2_form.pdf", // fileName
            testTaxYear, // taxYear
            null, // formType
            0.95, // confidenceScore
            new HashMap<>(), // fieldConfidence
            1, // sourcePage
            null, // extractionReason
            "test-owner", // owner
            "Test Employer", // employer
            "12-3456789", // employerEin
            null, // employerAddress
            null, // employerCounty
            null, // totalMonthsInCity
            "Test Employee", // employee
            null, // employeeInfo
            federalWages, // federalWages
            null, // medicareWages
            localWages, // localWages
            null, // localWithheld
            "Dublin", // locality
            null, // taxDue
            new ArrayList<>() // lowConfidenceFields
        );
    }
}
