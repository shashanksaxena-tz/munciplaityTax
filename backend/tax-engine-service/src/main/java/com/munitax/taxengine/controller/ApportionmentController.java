package com.munitax.taxengine.controller;

import com.munitax.taxengine.domain.apportionment.ApportionmentFormula;
import com.munitax.taxengine.dto.ApportionmentBreakdownDto;
import com.munitax.taxengine.service.ApportionmentService;
import com.munitax.taxengine.service.PropertyFactorService;
import com.munitax.taxengine.service.PayrollFactorService;
import com.munitax.taxengine.service.SalesFactorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for apportionment calculation operations.
 * Provides endpoints for calculating apportionment percentages with different formulas.
 */
@Slf4j
@RestController
@RequestMapping("/api/apportionment")
@RequiredArgsConstructor
public class ApportionmentController {

    private final ApportionmentService apportionmentService;
    private final PropertyFactorService propertyFactorService;
    private final PayrollFactorService payrollFactorService;
    private final SalesFactorService salesFactorService;

    /**
     * T113: Calculate apportionment percentage with detailed breakdown.
     * POST /api/apportionment/calculate
     *
     * @param request Apportionment calculation request
     * @return Apportionment breakdown with factor contributions
     */
    @PostMapping("/calculate")
    public ResponseEntity<ApportionmentBreakdownDto> calculateApportionment(
            @Valid @RequestBody ApportionmentCalculationRequest request) {

        log.info("Calculating apportionment with formula: {}", request.getFormula());

        try {
            // Validate input percentages
            apportionmentService.validateFactorPercentage(request.getPropertyFactorPercentage());
            apportionmentService.validateFactorPercentage(request.getPayrollFactorPercentage());
            apportionmentService.validateFactorPercentage(request.getSalesFactorPercentage());

            // Calculate apportionment breakdown
            Map<String, Object> breakdown = apportionmentService.calculateApportionmentBreakdown(
                    request.getPropertyFactorPercentage() != null ? request.getPropertyFactorPercentage() : BigDecimal.ZERO,
                    request.getPayrollFactorPercentage() != null ? request.getPayrollFactorPercentage() : BigDecimal.ZERO,
                    request.getSalesFactorPercentage(),
                    request.getFormula()
            );

            // Build response DTO
            ApportionmentBreakdownDto response = ApportionmentBreakdownDto.builder()
                    .propertyFactorPercentage((BigDecimal) breakdown.get("propertyFactorPercentage"))
                    .propertyFactorWeight((BigDecimal) breakdown.get("propertyFactorWeight"))
                    .propertyFactorWeightedContribution((BigDecimal) breakdown.get("propertyFactorWeightedContribution"))
                    .payrollFactorPercentage((BigDecimal) breakdown.get("payrollFactorPercentage"))
                    .payrollFactorWeight((BigDecimal) breakdown.get("payrollFactorWeight"))
                    .payrollFactorWeightedContribution((BigDecimal) breakdown.get("payrollFactorWeightedContribution"))
                    .salesFactorPercentage((BigDecimal) breakdown.get("salesFactorPercentage"))
                    .salesFactorWeight((BigDecimal) breakdown.get("salesFactorWeight"))
                    .salesFactorWeightedContribution((BigDecimal) breakdown.get("salesFactorWeightedContribution"))
                    .totalWeight((BigDecimal) breakdown.get("totalWeight"))
                    .finalApportionmentPercentage((BigDecimal) breakdown.get("finalApportionmentPercentage"))
                    .build();

            log.info("Apportionment calculated: {}%", response.getFinalApportionmentPercentage());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error calculating apportionment: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error calculating apportionment", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error calculating apportionment: " + e.getMessage(), e);
        }
    }

    /**
     * Compare traditional formula vs single-sales-factor.
     * POST /api/apportionment/compare
     *
     * @param request Apportionment comparison request
     * @return Comparison results with recommendation
     */
    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareFormulas(
            @Valid @RequestBody ApportionmentComparisonRequest request) {

        log.info("Comparing formulas: Traditional={}, Single-Sales-Factor", request.getTraditionalFormula());

        try {
            Map<String, Object> comparison = apportionmentService.compareFormulaOptions(
                    request.getPropertyFactorPercentage() != null ? request.getPropertyFactorPercentage() : BigDecimal.ZERO,
                    request.getPayrollFactorPercentage() != null ? request.getPayrollFactorPercentage() : BigDecimal.ZERO,
                    request.getSalesFactorPercentage(),
                    request.getTraditionalFormula()
            );

            return ResponseEntity.ok(comparison);

        } catch (IllegalArgumentException e) {
            log.error("Validation error comparing formulas: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error comparing formulas", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error comparing formulas: " + e.getMessage(), e);
        }
    }

    /**
     * Request DTO for apportionment calculation.
     */
    public static class ApportionmentCalculationRequest {
        private BigDecimal propertyFactorPercentage;
        private BigDecimal payrollFactorPercentage;
        private BigDecimal salesFactorPercentage;
        private ApportionmentFormula formula;

        public BigDecimal getPropertyFactorPercentage() {
            return propertyFactorPercentage;
        }

        public void setPropertyFactorPercentage(BigDecimal propertyFactorPercentage) {
            this.propertyFactorPercentage = propertyFactorPercentage;
        }

        public BigDecimal getPayrollFactorPercentage() {
            return payrollFactorPercentage;
        }

        public void setPayrollFactorPercentage(BigDecimal payrollFactorPercentage) {
            this.payrollFactorPercentage = payrollFactorPercentage;
        }

        public BigDecimal getSalesFactorPercentage() {
            return salesFactorPercentage;
        }

        public void setSalesFactorPercentage(BigDecimal salesFactorPercentage) {
            this.salesFactorPercentage = salesFactorPercentage;
        }

        public ApportionmentFormula getFormula() {
            return formula;
        }

        public void setFormula(ApportionmentFormula formula) {
            this.formula = formula;
        }
    }

    /**
     * Request DTO for formula comparison.
     */
    public static class ApportionmentComparisonRequest {
        private BigDecimal propertyFactorPercentage;
        private BigDecimal payrollFactorPercentage;
        private BigDecimal salesFactorPercentage;
        private ApportionmentFormula traditionalFormula;

        public BigDecimal getPropertyFactorPercentage() {
            return propertyFactorPercentage;
        }

        public void setPropertyFactorPercentage(BigDecimal propertyFactorPercentage) {
            this.propertyFactorPercentage = propertyFactorPercentage;
        }

        public BigDecimal getPayrollFactorPercentage() {
            return payrollFactorPercentage;
        }

        public void setPayrollFactorPercentage(BigDecimal payrollFactorPercentage) {
            this.payrollFactorPercentage = payrollFactorPercentage;
        }

        public BigDecimal getSalesFactorPercentage() {
            return salesFactorPercentage;
        }

        public void setSalesFactorPercentage(BigDecimal salesFactorPercentage) {
            this.salesFactorPercentage = salesFactorPercentage;
        }

        public ApportionmentFormula getTraditionalFormula() {
            return traditionalFormula;
        }

        public void setTraditionalFormula(ApportionmentFormula traditionalFormula) {
            this.traditionalFormula = traditionalFormula;
        }
    }
}
