package com.munitax.ledger.controller;

import com.munitax.ledger.dto.TrialBalanceResponse;
import com.munitax.ledger.service.TrialBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for TrialBalanceController
 * T043: Integration test for trial balance API endpoint
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class TrialBalanceControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TrialBalanceService trialBalanceService;
    
    private UUID tenantId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }
    
    /**
     * T043: Test GET /api/v1/trial-balance endpoint
     */
    @Test
    void testGetTrialBalance_Success() throws Exception {
        // Given
        LocalDate asOfDate = LocalDate.of(2024, 12, 31);
        
        TrialBalanceResponse mockResponse = TrialBalanceResponse.builder()
                .asOfDate(asOfDate)
                .accounts(new ArrayList<>())
                .totalDebits(new BigDecimal("3500000.00"))
                .totalCredits(new BigDecimal("3500000.00"))
                .difference(BigDecimal.ZERO)
                .isBalanced(true)
                .status("BALANCED")
                .accountsByType(new HashMap<>())
                .totalsByType(new HashMap<>())
                .accountCount(10)
                .tenantId(tenantId.toString())
                .entityId(UUID.randomUUID().toString())
                .generatedAt(LocalDate.now().toString())
                .build();
        
        when(trialBalanceService.generateTrialBalance(eq(tenantId), eq(asOfDate)))
                .thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/trial-balance")
                        .param("tenantId", tenantId.toString())
                        .param("asOfDate", asOfDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOfDate").value(asOfDate.toString()))
                .andExpect(jsonPath("$.totalDebits").value(3500000.00))
                .andExpect(jsonPath("$.totalCredits").value(3500000.00))
                .andExpect(jsonPath("$.difference").value(0))
                .andExpect(jsonPath("$.isBalanced").value(true))
                .andExpect(jsonPath("$.status").value("BALANCED"))
                .andExpect(jsonPath("$.accountCount").value(10));
    }
    
    /**
     * T043: Test GET /api/v1/trial-balance without asOfDate (defaults to current date)
     */
    @Test
    void testGetTrialBalance_DefaultDate() throws Exception {
        // Given
        TrialBalanceResponse mockResponse = TrialBalanceResponse.builder()
                .asOfDate(LocalDate.now())
                .accounts(new ArrayList<>())
                .totalDebits(BigDecimal.ZERO)
                .totalCredits(BigDecimal.ZERO)
                .difference(BigDecimal.ZERO)
                .isBalanced(true)
                .status("BALANCED")
                .accountsByType(new HashMap<>())
                .totalsByType(new HashMap<>())
                .accountCount(0)
                .tenantId(tenantId.toString())
                .build();
        
        when(trialBalanceService.generateTrialBalance(eq(tenantId), isNull()))
                .thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/trial-balance")
                        .param("tenantId", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBalanced").value(true));
    }
    
    /**
     * T043: Test GET /api/v1/trial-balance/period for quarter-end
     */
    @Test
    void testGetTrialBalanceForPeriod_QuarterEnd() throws Exception {
        // Given
        int year = 2024;
        String period = "Q1";
        LocalDate expectedDate = LocalDate.of(2024, 3, 31);
        
        TrialBalanceResponse mockResponse = TrialBalanceResponse.builder()
                .asOfDate(expectedDate)
                .accounts(new ArrayList<>())
                .totalDebits(new BigDecimal("1000000.00"))
                .totalCredits(new BigDecimal("1000000.00"))
                .difference(BigDecimal.ZERO)
                .isBalanced(true)
                .status("BALANCED")
                .accountsByType(new HashMap<>())
                .totalsByType(new HashMap<>())
                .accountCount(8)
                .tenantId(tenantId.toString())
                .build();
        
        when(trialBalanceService.generateTrialBalanceForPeriod(eq(tenantId), eq(year), eq(period)))
                .thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/trial-balance/period")
                        .param("tenantId", tenantId.toString())
                        .param("year", String.valueOf(year))
                        .param("period", period))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOfDate").value(expectedDate.toString()))
                .andExpect(jsonPath("$.isBalanced").value(true));
    }
    
    /**
     * T043: Test GET /api/v1/trial-balance/period for year-end
     */
    @Test
    void testGetTrialBalanceForPeriod_YearEnd() throws Exception {
        // Given
        int year = 2024;
        String period = "YEAR";
        LocalDate expectedDate = LocalDate.of(2024, 12, 31);
        
        TrialBalanceResponse mockResponse = TrialBalanceResponse.builder()
                .asOfDate(expectedDate)
                .accounts(new ArrayList<>())
                .totalDebits(new BigDecimal("5000000.00"))
                .totalCredits(new BigDecimal("5000000.00"))
                .difference(BigDecimal.ZERO)
                .isBalanced(true)
                .status("BALANCED")
                .accountsByType(new HashMap<>())
                .totalsByType(new HashMap<>())
                .accountCount(12)
                .tenantId(tenantId.toString())
                .build();
        
        when(trialBalanceService.generateTrialBalanceForPeriod(eq(tenantId), eq(year), eq(period)))
                .thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/trial-balance/period")
                        .param("tenantId", tenantId.toString())
                        .param("year", String.valueOf(year))
                        .param("period", period))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOfDate").value(expectedDate.toString()))
                .andExpect(jsonPath("$.status").value("BALANCED"));
    }
    
    /**
     * T042: Test unbalanced trial balance detection through API
     */
    @Test
    void testGetTrialBalance_Unbalanced() throws Exception {
        // Given - Simulate unbalanced ledger
        LocalDate asOfDate = LocalDate.now();
        
        TrialBalanceResponse mockResponse = TrialBalanceResponse.builder()
                .asOfDate(asOfDate)
                .accounts(new ArrayList<>())
                .totalDebits(new BigDecimal("3500000.00"))
                .totalCredits(new BigDecimal("3499950.00"))
                .difference(new BigDecimal("50.00"))
                .isBalanced(false)
                .status("UNBALANCED")
                .accountsByType(new HashMap<>())
                .totalsByType(new HashMap<>())
                .accountCount(10)
                .tenantId(tenantId.toString())
                .build();
        
        when(trialBalanceService.generateTrialBalance(eq(tenantId), any(LocalDate.class)))
                .thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/trial-balance")
                        .param("tenantId", tenantId.toString())
                        .param("asOfDate", asOfDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBalanced").value(false))
                .andExpect(jsonPath("$.status").value("UNBALANCED"))
                .andExpect(jsonPath("$.difference").value(50.00));
    }
    
    /**
     * T043: Test missing required parameter
     */
    @Test
    void testGetTrialBalance_MissingTenantId() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/trial-balance"))
                .andExpect(status().is4xxClientError());
    }
}
