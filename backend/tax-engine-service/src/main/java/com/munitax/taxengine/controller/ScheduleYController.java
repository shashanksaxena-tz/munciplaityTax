package com.munitax.taxengine.controller;

import com.munitax.taxengine.domain.apportionment.*;
import com.munitax.taxengine.dto.*;
import com.munitax.taxengine.repository.*;
import com.munitax.taxengine.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Schedule Y multi-state sourcing operations.
 * Provides endpoints for filing, retrieving, and managing Schedule Y apportionment calculations.
 */
@Slf4j
@RestController
@RequestMapping("/api/schedule-y")
@RequiredArgsConstructor
public class ScheduleYController {

    private final ScheduleYRepository scheduleYRepository;
    private final PropertyFactorRepository propertyFactorRepository;
    private final PayrollFactorRepository payrollFactorRepository;
    private final SalesFactorRepository salesFactorRepository;
    private final ApportionmentAuditLogRepository auditLogRepository;
    
    private final ApportionmentService apportionmentService;
    private final SalesFactorService salesFactorService;
    private final NexusService nexusService;

    // TODO: Replace with actual authentication service
    private static final UUID MOCK_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    /**
     * T056: Create Schedule Y filing with apportionment calculation.
     * POST /api/schedule-y
     *
     * @param request Schedule Y filing request
     * @return Schedule Y response with calculated apportionment
     */
    @PostMapping
    public ResponseEntity<ScheduleYResponse> createScheduleY(@Valid @RequestBody ScheduleYRequest request) {
        log.info("Creating Schedule Y for business {} and tax year {}", request.getBusinessId(), request.getTaxYear());

        try {
            // Validate that filing doesn't already exist for this business and tax year
            boolean exists = scheduleYRepository.existsByBusinessIdAndTaxYearAndTenantId(
                    request.getBusinessId(), request.getTaxYear(), MOCK_TENANT_ID);
            
            if (exists && !Boolean.TRUE.equals(request.getIsAmended())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Schedule Y already exists for business " + request.getBusinessId() + 
                        " and tax year " + request.getTaxYear());
            }

            // Create ScheduleY entity
            ScheduleY scheduleY = new ScheduleY();
            scheduleY.setReturnId(request.getReturnId());
            scheduleY.setTenantId(MOCK_TENANT_ID);
            scheduleY.setTaxYear(request.getTaxYear());
            scheduleY.setApportionmentFormula(request.getApportionmentFormula());
            scheduleY.setSourcingMethodElection(request.getSourcingMethodElection());
            scheduleY.setThrowbackElection(request.getThrowbackElection());
            scheduleY.setServiceSourcingMethod(request.getServiceSourcingMethod());
            scheduleY.setStatus("FILED");
            scheduleY.setCreatedDate(LocalDateTime.now());
            scheduleY.setCreatedBy(MOCK_USER_ID);
            scheduleY.setLastModifiedDate(LocalDateTime.now());
            scheduleY.setLastModifiedBy(MOCK_USER_ID);
            
            if (Boolean.TRUE.equals(request.getIsAmended()) && request.getOriginalScheduleYId() != null) {
                scheduleY.setAmendsScheduleYId(request.getOriginalScheduleYId());
            }

            // Calculate sales factor with sourcing election
            BigDecimal salesFactorPercentage = calculateSalesFactor(request.getSalesFactor(), 
                    request.getSourcingMethodElection());
            scheduleY.setSalesFactorPercentage(salesFactorPercentage);

            // Calculate property and payroll factors
            BigDecimal propertyFactorPercentage = calculatePropertyFactor(request.getPropertyFactor());
            scheduleY.setPropertyFactorPercentage(propertyFactorPercentage);

            BigDecimal payrollFactorPercentage = calculatePayrollFactor(request.getPayrollFactor());
            scheduleY.setPayrollFactorPercentage(payrollFactorPercentage);

            // Calculate final apportionment percentage
            BigDecimal finalApportionment = apportionmentService.calculateApportionmentPercentage(
                    propertyFactorPercentage != null ? propertyFactorPercentage : BigDecimal.ZERO,
                    payrollFactorPercentage != null ? payrollFactorPercentage : BigDecimal.ZERO,
                    salesFactorPercentage,
                    request.getApportionmentFormula()
            );
            scheduleY.setFinalApportionmentPercentage(finalApportionment);

            // Save Schedule Y
            ScheduleY savedScheduleY = scheduleYRepository.save(scheduleY);
            log.info("Schedule Y created with ID: {}", savedScheduleY.getScheduleYId());

            // Save factor details
            saveFactorDetails(savedScheduleY, request);

            // Create audit log entry
            createAuditLog(savedScheduleY, "CREATED", null, 
                    "Schedule Y filed for tax year " + request.getTaxYear());

            // Build and return response
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(buildScheduleYResponse(savedScheduleY, request));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating Schedule Y: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error creating Schedule Y", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error creating Schedule Y: " + e.getMessage(), e);
        }
    }

    /**
     * T057: List Schedule Y filings with pagination.
     * GET /api/schedule-y?businessId={businessId}&page={page}&size={size}
     *
     * @param businessId Business ID to filter by (optional)
     * @param taxYear Tax year to filter by (optional)
     * @param page Page number (default 0)
     * @param size Page size (default 20)
     * @return Page of Schedule Y filings
     */
    @GetMapping
    public ResponseEntity<Page<ScheduleYResponse>> listScheduleY(
            @RequestParam(required = false) UUID businessId,
            @RequestParam(required = false) Integer taxYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Listing Schedule Y filings: businessId={}, taxYear={}, page={}, size={}", 
                businessId, taxYear, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("taxYear").descending());
            Page<ScheduleY> scheduleYPage;

            if (businessId != null) {
                scheduleYPage = scheduleYRepository.findByBusinessIdAndTenantIdOrderByTaxYearDesc(
                        businessId, MOCK_TENANT_ID, pageable);
            } else if (taxYear != null) {
                scheduleYPage = scheduleYRepository.findByTaxYearAndTenantId(
                        taxYear, MOCK_TENANT_ID, pageable);
            } else {
                scheduleYPage = scheduleYRepository.findAll(pageable);
            }

            Page<ScheduleYResponse> responsePage = scheduleYPage.map(this::buildScheduleYResponseSimple);
            
            return ResponseEntity.ok(responsePage);

        } catch (Exception e) {
            log.error("Error listing Schedule Y filings", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error listing Schedule Y filings: " + e.getMessage(), e);
        }
    }

    /**
     * T058: Get Schedule Y details by ID.
     * GET /api/schedule-y/{id}
     *
     * @param id Schedule Y ID
     * @return Schedule Y response with full details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleYResponse> getScheduleY(@PathVariable UUID id) {
        log.info("Getting Schedule Y with ID: {}", id);

        try {
            ScheduleY scheduleY = scheduleYRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Schedule Y not found with ID: " + id));

            // Verify tenant access
            if (!MOCK_TENANT_ID.equals(scheduleY.getTenantId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Access denied to Schedule Y: " + id);
            }

            ScheduleYResponse response = buildScheduleYResponseSimple(scheduleY);
            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting Schedule Y", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error getting Schedule Y: " + e.getMessage(), e);
        }
    }

    /**
     * Get detailed breakdown of apportionment calculation.
     * GET /api/schedule-y/{id}/breakdown
     *
     * @param id Schedule Y ID
     * @return Apportionment breakdown details
     */
    @GetMapping("/{id}/breakdown")
    public ResponseEntity<ApportionmentBreakdownDto> getApportionmentBreakdown(@PathVariable UUID id) {
        log.info("Getting apportionment breakdown for Schedule Y: {}", id);

        try {
            ScheduleY scheduleY = scheduleYRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Schedule Y not found with ID: " + id));

            // Verify tenant access
            if (!MOCK_TENANT_ID.equals(scheduleY.getTenantId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Access denied to Schedule Y: " + id);
            }

            Map<String, BigDecimal> breakdown = apportionmentService.calculateApportionmentBreakdown(
                    scheduleY.getPropertyFactorPercentage() != null ? scheduleY.getPropertyFactorPercentage() : BigDecimal.ZERO,
                    scheduleY.getPayrollFactorPercentage() != null ? scheduleY.getPayrollFactorPercentage() : BigDecimal.ZERO,
                    scheduleY.getSalesFactorPercentage(),
                    scheduleY.getApportionmentFormula()
            );

            ApportionmentBreakdownDto response = ApportionmentBreakdownDto.builder()
                    .propertyFactorPercentage(breakdown.get("propertyFactorPercentage"))
                    .propertyFactorWeight(breakdown.get("propertyFactorWeight"))
                    .propertyFactorWeightedContribution(breakdown.get("propertyFactorWeightedContribution"))
                    .payrollFactorPercentage(breakdown.get("payrollFactorPercentage"))
                    .payrollFactorWeight(breakdown.get("payrollFactorWeight"))
                    .payrollFactorWeightedContribution(breakdown.get("payrollFactorWeightedContribution"))
                    .salesFactorPercentage(breakdown.get("salesFactorPercentage"))
                    .salesFactorWeight(breakdown.get("salesFactorWeight"))
                    .salesFactorWeightedContribution(breakdown.get("salesFactorWeightedContribution"))
                    .totalWeight(breakdown.get("totalWeight"))
                    .finalApportionmentPercentage(breakdown.get("finalApportionmentPercentage"))
                    .build();

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting apportionment breakdown", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error getting apportionment breakdown: " + e.getMessage(), e);
        }
    }

    /**
     * Get audit log for Schedule Y.
     * GET /api/schedule-y/{id}/audit-log
     *
     * @param id Schedule Y ID
     * @return List of audit log entries
     */
    @GetMapping("/{id}/audit-log")
    public ResponseEntity<Page<ApportionmentAuditLog>> getAuditLog(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting audit log for Schedule Y: {}", id);

        try {
            // Verify Schedule Y exists
            ScheduleY scheduleY = scheduleYRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Schedule Y not found with ID: " + id));

            // Verify tenant access
            if (!MOCK_TENANT_ID.equals(scheduleY.getTenantId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Access denied to Schedule Y: " + id);
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("changeDate").descending());
            Page<ApportionmentAuditLog> auditLogs = auditLogRepository.findByScheduleYIdAndTenantId(
                    id, MOCK_TENANT_ID, pageable);

            return ResponseEntity.ok(auditLogs);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting audit log", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error getting audit log: " + e.getMessage(), e);
        }
    }

    // ==================== Private Helper Methods ====================

    private BigDecimal calculateSalesFactor(SalesFactorDto salesFactor, 
                                           SourcingMethodElection sourcingMethod) {
        if (salesFactor == null || salesFactor.getTotalSalesEverywhere() == null 
                || salesFactor.getTotalSalesEverywhere().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal numerator = salesFactor.getSalesInOhio() != null 
                ? salesFactor.getSalesInOhio() : BigDecimal.ZERO;
        BigDecimal denominator = salesFactor.getTotalSalesEverywhere();

        return apportionmentService.calculateFactorPercentage(numerator, denominator);
    }

    private BigDecimal calculatePropertyFactor(PropertyFactorDto propertyFactor) {
        if (propertyFactor == null || propertyFactor.getTotalPropertyEverywhere() == null 
                || propertyFactor.getTotalPropertyEverywhere().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal numerator = propertyFactor.getPropertyInOhio() != null 
                ? propertyFactor.getPropertyInOhio() : BigDecimal.ZERO;
        BigDecimal denominator = propertyFactor.getTotalPropertyEverywhere();

        return apportionmentService.calculateFactorPercentage(numerator, denominator);
    }

    private BigDecimal calculatePayrollFactor(PayrollFactorDto payrollFactor) {
        if (payrollFactor == null || payrollFactor.getTotalPayrollEverywhere() == null 
                || payrollFactor.getTotalPayrollEverywhere().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal numerator = payrollFactor.getPayrollInOhio() != null 
                ? payrollFactor.getPayrollInOhio() : BigDecimal.ZERO;
        BigDecimal denominator = payrollFactor.getTotalPayrollEverywhere();

        return apportionmentService.calculateFactorPercentage(numerator, denominator);
    }

    private void saveFactorDetails(ScheduleY scheduleY, ScheduleYRequest request) {
        // Save Property Factor
        if (request.getPropertyFactor() != null) {
            PropertyFactor propertyFactor = new PropertyFactor();
            propertyFactor.setScheduleY(scheduleY);
            propertyFactor.setTenantId(MOCK_TENANT_ID);
            propertyFactor.setPropertyInOhio(request.getPropertyFactor().getPropertyInOhio());
            propertyFactor.setTotalPropertyEverywhere(request.getPropertyFactor().getTotalPropertyEverywhere());
            propertyFactor.setRentedPropertyInOhio(request.getPropertyFactor().getRentedPropertyInOhio());
            propertyFactor.setRentedPropertyEverywhere(request.getPropertyFactor().getRentedPropertyEverywhere());
            propertyFactor.setCreatedDate(LocalDateTime.now());
            propertyFactor.setCreatedBy(MOCK_USER_ID);
            propertyFactor.setLastModifiedDate(LocalDateTime.now());
            propertyFactorRepository.save(propertyFactor);
        }

        // Save Payroll Factor
        if (request.getPayrollFactor() != null) {
            PayrollFactor payrollFactor = new PayrollFactor();
            payrollFactor.setScheduleY(scheduleY);
            payrollFactor.setTenantId(MOCK_TENANT_ID);
            payrollFactor.setPayrollInOhio(request.getPayrollFactor().getPayrollInOhio());
            payrollFactor.setTotalPayrollEverywhere(request.getPayrollFactor().getTotalPayrollEverywhere());
            payrollFactor.setEmployeesInOhio(request.getPayrollFactor().getEmployeesInOhio());
            payrollFactor.setTotalEmployeesEverywhere(request.getPayrollFactor().getTotalEmployeesEverywhere());
            payrollFactor.setCreatedDate(LocalDateTime.now());
            payrollFactor.setCreatedBy(MOCK_USER_ID);
            payrollFactor.setLastModifiedDate(LocalDateTime.now());
            payrollFactorRepository.save(payrollFactor);
        }

        // Save Sales Factor
        if (request.getSalesFactor() != null) {
            SalesFactor salesFactor = new SalesFactor();
            salesFactor.setScheduleY(scheduleY);
            salesFactor.setTenantId(MOCK_TENANT_ID);
            salesFactor.setSalesInOhio(request.getSalesFactor().getSalesInOhio());
            salesFactor.setTotalSalesEverywhere(request.getSalesFactor().getTotalSalesEverywhere());
            salesFactor.setCreatedDate(LocalDateTime.now());
            salesFactor.setCreatedBy(MOCK_USER_ID);
            salesFactor.setLastModifiedDate(LocalDateTime.now());
            salesFactorRepository.save(salesFactor);
        }
    }

    private void createAuditLog(ScheduleY scheduleY, String changeType, String fieldName, String description) {
        ApportionmentAuditLog auditLog = new ApportionmentAuditLog();
        auditLog.setScheduleYId(scheduleY.getScheduleYId());
        auditLog.setTenantId(MOCK_TENANT_ID);
        auditLog.setChangeType(AuditChangeType.valueOf(changeType));
        auditLog.setEntityType(fieldName);
        auditLog.setChangeReason(description);
        auditLog.setChangedBy(MOCK_USER_ID);
        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {}", description);
    }

    private ScheduleYResponse buildScheduleYResponse(ScheduleY scheduleY, ScheduleYRequest request) {
        return ScheduleYResponse.builder()
                .id(scheduleY.getScheduleYId())
                .businessId(request.getBusinessId())
                .returnId(scheduleY.getReturnId())
                .taxYear(scheduleY.getTaxYear())
                .apportionmentFormula(scheduleY.getApportionmentFormula())
                .sourcingMethodElection(scheduleY.getSourcingMethodElection())
                .throwbackElection(scheduleY.getThrowbackElection())
                .serviceSourcingMethod(scheduleY.getServiceSourcingMethod())
                .apportionmentPercentage(scheduleY.getFinalApportionmentPercentage())
                .propertyFactor(request.getPropertyFactor())
                .payrollFactor(request.getPayrollFactor())
                .salesFactor(request.getSalesFactor())
                .notes(request.getNotes())
                .isAmended(request.getIsAmended())
                .originalScheduleYId(request.getOriginalScheduleYId())
                .filedAt(scheduleY.getCreatedDate())
                .filedBy(scheduleY.getCreatedBy())
                .createdAt(scheduleY.getCreatedDate())
                .updatedAt(scheduleY.getLastModifiedDate())
                .updatedBy(scheduleY.getLastModifiedBy())
                .build();
    }

    private ScheduleYResponse buildScheduleYResponseSimple(ScheduleY scheduleY) {
        return ScheduleYResponse.builder()
                .id(scheduleY.getScheduleYId())
                .returnId(scheduleY.getReturnId())
                .taxYear(scheduleY.getTaxYear())
                .apportionmentFormula(scheduleY.getApportionmentFormula())
                .sourcingMethodElection(scheduleY.getSourcingMethodElection())
                .throwbackElection(scheduleY.getThrowbackElection())
                .serviceSourcingMethod(scheduleY.getServiceSourcingMethod())
                .apportionmentPercentage(scheduleY.getFinalApportionmentPercentage())
                .isAmended(scheduleY.getAmendsScheduleYId() != null)
                .originalScheduleYId(scheduleY.getAmendsScheduleYId())
                .filedAt(scheduleY.getCreatedDate())
                .filedBy(scheduleY.getCreatedBy())
                .createdAt(scheduleY.getCreatedDate())
                .updatedAt(scheduleY.getLastModifiedDate())
                .updatedBy(scheduleY.getLastModifiedBy())
                .build();
    }
}
