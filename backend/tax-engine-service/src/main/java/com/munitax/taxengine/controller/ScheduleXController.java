package com.munitax.taxengine.controller;

import com.munitax.taxengine.dto.BusinessScheduleXDetailsDto;
import com.munitax.taxengine.dto.MultiYearComparisonDto;
import com.munitax.taxengine.dto.ScheduleXAutoCalcRequest;
import com.munitax.taxengine.dto.ScheduleXAutoCalcResponse;
import com.munitax.taxengine.service.ScheduleXAutoCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Schedule X Controller (T021)
 * 
 * Provides API endpoints for Schedule X operations:
 * - POST /api/schedule-x/auto-calculate - Auto-calculation helpers
 * - GET /api/schedule-x/multi-year-comparison - Multi-year comparison
 * - POST /api/schedule-x/import-from-federal - Import from uploaded Form 1120
 */
@RestController
@RequestMapping("/api/schedule-x")
@CrossOrigin(origins = "*")
public class ScheduleXController {
    
    private final ScheduleXAutoCalculationService autoCalculationService;
    
    public ScheduleXController(ScheduleXAutoCalculationService autoCalculationService) {
        this.autoCalculationService = autoCalculationService;
    }
    
    /**
     * Auto-calculate Schedule X field values (FR-031)
     * 
     * Examples:
     * - Meals & Entertainment: federalMeals → municipal add-back (× 2)
     * - 5% Rule: interest + dividends + capitalGains → 5% add-back
     * - Related-Party Excess: paid - FMV → excess
     * 
     * @param request Auto-calculation request with field name and inputs
     * @return Calculated value with explanation
     */
    @PostMapping("/auto-calculate")
    public ResponseEntity<ScheduleXAutoCalcResponse> autoCalculate(
            @RequestBody ScheduleXAutoCalcRequest request) {
        
        var autoCalcRequest = new ScheduleXAutoCalculationService.AutoCalcRequest(
            request.fieldName(),
            request.inputs()
        );
        
        var result = autoCalculationService.autoCalculate(autoCalcRequest);
        
        var response = new ScheduleXAutoCalcResponse(
            result.calculatedValue(),
            result.explanation(),
            result.details()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get multi-year comparison of Schedule X data (FR-038)
     * 
     * @param businessId Business ID
     * @param years Comma-separated years (e.g., "2024,2023,2022")
     * @return Schedule X data for requested years
     */
    @GetMapping("/multi-year-comparison")
    public ResponseEntity<MultiYearComparisonDto> getMultiYearComparison(
            @RequestParam String businessId,
            @RequestParam String years) {
        
        // TODO: Implement multi-year query
        // Parse years, query database, return Schedule X for each year
        
        // Stub response
        var yearsList = List.of(2024, 2023, 2022);
        var emptyData = List.<BusinessScheduleXDetailsDto>of();
        
        return ResponseEntity.ok(new MultiYearComparisonDto(yearsList, emptyData));
    }
    
    /**
     * Import Schedule X fields from uploaded Form 1120/1065 PDF (FR-032)
     * 
     * @param returnId Return ID to associate with
     * @param federalFormPdfUrl URL of uploaded federal form PDF
     * @return Extraction result with confidence scores
     */
    @PostMapping("/import-from-federal")
    public ResponseEntity<String> importFromFederal(
            @RequestParam String returnId,
            @RequestParam String federalFormPdfUrl) {
        
        // TODO: Implement AI extraction via extraction-service
        // Call GeminiExtractionService with PDF URL
        // Return ScheduleXExtractionResult with confidence scores and bounding boxes
        
        return ResponseEntity.ok("Import from federal form initiated. Extraction ID: " + returnId);
    }
}
