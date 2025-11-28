package com.munitax.taxengine.controller;

import com.munitax.taxengine.domain.nol.*;
import com.munitax.taxengine.dto.*;
import com.munitax.taxengine.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for Net Operating Loss (NOL) operations.
 * 
 * Endpoints:
 * - POST /api/nol - Create new NOL
 * - GET /api/nol/{businessId} - Get NOLs for business
 * - GET /api/nol/{businessId}/available - Get available NOL balance
 * - POST /api/nol/apply - Apply NOL deduction to return
 * - GET /api/nol/schedule/{returnId} - Get NOL schedule for return
 * - GET /api/nol/schedule/{businessId}/vintages/{taxYear} - Get vintage breakdown
 * - POST /api/nol/carryback - Elect NOL carryback
 * - GET /api/nol/carryback/{nolId} - Get carryback summary
 * - GET /api/nol/alerts/{businessId} - Get expiration alerts
 * 
 * @see NOLService
 * @see NOLCarrybackService
 * @see NOLScheduleService
 */
@RestController
@RequestMapping("/api/nol")
@RequiredArgsConstructor
@Slf4j
public class NOLController {
    
    private final NOLService nolService;
    private final NOLCarrybackService nolCarrybackService;
    private final NOLScheduleService nolScheduleService;
    private final NOLRepository nolRepository;
    private final NOLExpirationAlertRepository nolExpirationAlertRepository;
    
    /**
     * Create a new NOL record for a business with net operating loss.
     * 
     * POST /api/nol
     * 
     * @param request Create NOL request
     * @param userId User ID from JWT (header) - NOTE: In production, extract from validated JWT token via Spring Security
     * @param tenantId Tenant ID from JWT (header) - NOTE: In production, extract from validated JWT token via Spring Security
     * @return Created NOL response
     */
    @PostMapping
    public ResponseEntity<NOLResponse> createNOL(
            @Valid @RequestBody CreateNOLRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Creating NOL for business {} tax year {}", request.getBusinessId(), request.getTaxYear());
        
        NOL nol = nolService.createNOL(
            request.getBusinessId(),
            request.getTaxYear(),
            request.getLossAmount(),
            request.getJurisdiction(),
            request.getEntityType(),
            request.getApportionmentPercentage(),
            request.getMunicipalityCode(),
            userId,
            tenantId
        );
        
        NOLResponse response = mapNOLToResponse(nol);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get all NOLs for a business.
     * 
     * GET /api/nol/{businessId}
     * 
     * @param businessId Business ID
     * @param jurisdiction Optional jurisdiction filter
     * @return List of NOL responses
     */
    @GetMapping("/{businessId}")
    public ResponseEntity<List<NOLResponse>> getNOLs(
            @PathVariable UUID businessId,
            @RequestParam(required = false) Jurisdiction jurisdiction) {
        
        log.info("Getting NOLs for business {} jurisdiction {}", businessId, jurisdiction);
        
        List<NOL> nols = jurisdiction != null ?
            nolRepository.findByBusinessIdAndJurisdictionOrderByTaxYearAsc(businessId, jurisdiction) :
            nolRepository.findByBusinessIdOrderByTaxYearAsc(businessId);
        
        List<NOLResponse> responses = nols.stream()
            .map(this::mapNOLToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get available NOL balance for a business.
     * 
     * GET /api/nol/{businessId}/available
     * 
     * @param businessId Business ID
     * @param jurisdiction Optional jurisdiction filter
     * @return Available NOL balance
     */
    @GetMapping("/{businessId}/available")
    public ResponseEntity<Map<String, Object>> getAvailableNOLBalance(
            @PathVariable UUID businessId,
            @RequestParam(required = false) Jurisdiction jurisdiction) {
        
        log.info("Getting available NOL balance for business {} jurisdiction {}", businessId, jurisdiction);
        
        java.math.BigDecimal availableBalance = nolService.calculateAvailableNOLBalance(businessId, jurisdiction);
        
        Map<String, Object> response = new HashMap<>();
        response.put("businessId", businessId);
        response.put("jurisdiction", jurisdiction);
        response.put("availableBalance", availableBalance);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Apply NOL deduction to a tax return.
     * 
     * POST /api/nol/apply
     * 
     * @param request Apply NOL request
     * @param tenantId Tenant ID from JWT
     * @return List of NOL usage records created
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyNOLDeduction(
            @Valid @RequestBody ApplyNOLRequest request,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Applying NOL deduction for business {} return {}", 
                 request.getBusinessId(), request.getReturnId());
        
        List<NOLUsage> usages = nolService.applyNOLDeduction(
            request.getBusinessId(),
            request.getReturnId(),
            request.getTaxYear(),
            request.getTaxableIncomeBeforeNOL(),
            request.getNolDeductionAmount(),
            request.getTaxRate(),
            request.getJurisdiction(),
            tenantId
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("returnId", request.getReturnId());
        response.put("nolDeductionApplied", request.getNolDeductionAmount());
        response.put("taxableIncomeAfterNOL", 
                     request.getTaxableIncomeBeforeNOL().subtract(request.getNolDeductionAmount()));
        response.put("vintagesUsed", usages.size());
        response.put("usages", usages);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get NOL schedule for a tax return.
     * 
     * GET /api/nol/schedule/{returnId}
     * 
     * @param returnId Tax return ID
     * @return NOL schedule response
     */
    @GetMapping("/schedule/{returnId}")
    public ResponseEntity<NOLScheduleResponse> getNOLSchedule(@PathVariable UUID returnId) {
        
        log.info("Getting NOL schedule for return {}", returnId);
        
        Optional<NOLSchedule> scheduleOpt = nolScheduleService.getScheduleForReturn(returnId);
        
        if (scheduleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        NOLSchedule schedule = scheduleOpt.get();
        NOLScheduleResponse response = mapScheduleToResponse(schedule);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get NOL vintage breakdown for a business and tax year.
     * 
     * GET /api/nol/schedule/{businessId}/vintages/{taxYear}
     * 
     * @param businessId Business ID
     * @param taxYear Tax year
     * @return NOL vintage breakdown
     */
    @GetMapping("/schedule/{businessId}/vintages/{taxYear}")
    public ResponseEntity<List<NOLScheduleResponse.NOLVintageResponse>> getNOLVintageBreakdown(
            @PathVariable UUID businessId,
            @PathVariable Integer taxYear) {
        
        log.info("Getting NOL vintage breakdown for business {} year {}", businessId, taxYear);
        
        List<NOLScheduleService.NOLVintageDetail> vintages = 
            nolScheduleService.getNOLVintageBreakdown(businessId, taxYear);
        
        List<NOLScheduleResponse.NOLVintageResponse> responses = vintages.stream()
            .map(v -> NOLScheduleResponse.NOLVintageResponse.builder()
                .taxYear(v.getTaxYear())
                .originalAmount(v.getOriginalAmount())
                .previouslyUsed(v.getPreviouslyUsed())
                .expired(v.getExpired())
                .availableThisYear(v.getAvailableThisYear())
                .usedThisYear(v.getUsedThisYear())
                .remainingForFuture(v.getRemainingForFuture())
                .expirationDate(v.getExpirationDate() != null ? v.getExpirationDate().toString() : null)
                .isCarriedBack(v.getIsCarriedBack())
                .carrybackAmount(v.getCarrybackAmount())
                .build())
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Elect NOL carryback (CARES Act provision).
     * 
     * POST /api/nol/carryback
     * 
     * @param request Carryback election request
     * @param tenantId Tenant ID from JWT
     * @return Carryback election response
     */
    @PostMapping("/carryback")
    public ResponseEntity<CarrybackElectionResponse> electCarryback(
            @Valid @RequestBody CarrybackElectionRequest request,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        
        log.info("Processing carryback election for NOL {}", request.getNolId());
        
        // Convert DTOs to service model
        Map<Integer, NOLCarrybackService.PriorYearData> priorYearData = new HashMap<>();
        request.getPriorYearData().forEach((year, dto) -> {
            priorYearData.put(year, new NOLCarrybackService.PriorYearData(
                dto.getTaxableIncome(),
                dto.getTaxRate(),
                dto.getTaxPaid(),
                dto.getReturnId()
            ));
        });
        
        List<NOLCarryback> carrybacks = nolCarrybackService.processCarrybackElection(
            request.getNolId(),
            priorYearData,
            tenantId
        );
        
        // Get updated NOL
        NOL nol = nolRepository.findById(request.getNolId())
            .orElseThrow(() -> new IllegalArgumentException("NOL not found"));
        
        CarrybackElectionResponse response = CarrybackElectionResponse.builder()
            .nolId(nol.getId())
            .nolTaxYear(nol.getTaxYear())
            .totalNOLCarriedBack(nol.getCarrybackAmount())
            .totalRefund(nol.getCarrybackRefund())
            .remainingNOL(nol.getCurrentNOLBalance())
            .carrybackDetails(carrybacks.stream()
                .map(cb -> CarrybackElectionResponse.CarrybackYearDetail.builder()
                    .carrybackId(cb.getId())
                    .carrybackYear(cb.getCarrybackYear())
                    .priorYearTaxableIncome(cb.getPriorYearTaxableIncome())
                    .nolApplied(cb.getNolApplied())
                    .priorYearTaxRate(cb.getPriorYearTaxRate())
                    .refundAmount(cb.getRefundAmount())
                    .refundStatus(cb.getRefundStatus().name())
                    .filedDate(cb.getFiledDate())
                    .refundDate(cb.getRefundDate())
                    .build())
                .collect(Collectors.toList()))
            .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get carryback summary for an NOL.
     * 
     * GET /api/nol/carryback/{nolId}
     * 
     * @param nolId NOL ID
     * @return Carryback summary
     */
    @GetMapping("/carryback/{nolId}")
    public ResponseEntity<List<NOLCarryback>> getCarrybackSummary(@PathVariable UUID nolId) {
        
        log.info("Getting carryback summary for NOL {}", nolId);
        
        List<NOLCarryback> carrybacks = nolCarrybackService.getCarrybackSummary(nolId);
        
        return ResponseEntity.ok(carrybacks);
    }
    
    /**
     * Get expiration alerts for a business.
     * 
     * GET /api/nol/alerts/{businessId}
     * 
     * @param businessId Business ID
     * @param undismissedOnly Optional filter for undismissed alerts only
     * @return List of expiration alerts
     */
    @GetMapping("/alerts/{businessId}")
    public ResponseEntity<List<NOLExpirationAlert>> getExpirationAlerts(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "true") boolean undismissedOnly) {
        
        log.info("Getting expiration alerts for business {} (undismissed only: {})", 
                 businessId, undismissedOnly);
        
        List<NOLExpirationAlert> alerts = undismissedOnly ?
            nolExpirationAlertRepository.findUndismissedAlertsByBusinessId(businessId) :
            nolExpirationAlertRepository.findByBusinessIdOrderByExpirationDateAsc(businessId);
        
        return ResponseEntity.ok(alerts);
    }
    
    /**
     * Map NOL entity to response DTO.
     */
    private NOLResponse mapNOLToResponse(NOL nol) {
        return NOLResponse.builder()
            .id(nol.getId())
            .businessId(nol.getBusinessId())
            .taxYear(nol.getTaxYear())
            .jurisdiction(nol.getJurisdiction().name())
            .municipalityCode(nol.getMunicipalityCode())
            .entityType(nol.getEntityType().name())
            .originalNOLAmount(nol.getOriginalNOLAmount())
            .currentNOLBalance(nol.getCurrentNOLBalance())
            .usedAmount(nol.getUsedAmount())
            .expiredAmount(nol.getExpiredAmount())
            .expirationDate(nol.getExpirationDate())
            .carryforwardYears(nol.getCarryforwardYears())
            .isExpired(nol.isExpired())
            .hasRemainingBalance(nol.hasRemainingBalance())
            .isCarriedBack(nol.getIsCarriedBack())
            .carrybackAmount(nol.getCarrybackAmount())
            .carrybackRefund(nol.getCarrybackRefund())
            .apportionmentPercentage(nol.getApportionmentPercentage())
            .createdAt(nol.getCreatedAt())
            .updatedAt(nol.getUpdatedAt())
            .build();
    }
    
    /**
     * Map NOL schedule entity to response DTO.
     */
    private NOLScheduleResponse mapScheduleToResponse(NOLSchedule schedule) {
        return NOLScheduleResponse.builder()
            .id(schedule.getId())
            .businessId(schedule.getBusinessId())
            .returnId(schedule.getReturnId())
            .taxYear(schedule.getTaxYear())
            .totalBeginningBalance(schedule.getTotalBeginningBalance())
            .newNOLGenerated(schedule.getNewNOLGenerated())
            .totalAvailableNOL(schedule.getTotalAvailableNOL())
            .nolDeduction(schedule.getNolDeduction())
            .expiredNOL(schedule.getExpiredNOL())
            .totalEndingBalance(schedule.getTotalEndingBalance())
            .limitationPercentage(schedule.getLimitationPercentage())
            .taxableIncomeBeforeNOL(schedule.getTaxableIncomeBeforeNOL())
            .taxableIncomeAfterNOL(schedule.getTaxableIncomeAfterNOL())
            .createdAt(schedule.getCreatedAt())
            .build();
    }
}
