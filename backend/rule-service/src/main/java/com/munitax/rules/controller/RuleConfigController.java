package com.munitax.rules.controller;

import com.munitax.rules.dto.CreateRuleRequest;
import com.munitax.rules.dto.RuleResponse;
import com.munitax.rules.dto.UpdateRuleRequest;
import com.munitax.rules.service.RuleFacadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for tax rule configuration and management.
 * Handles CRUD operations for tax rules with temporal and multi-tenant support.
 */
@RestController
@RequestMapping("/api/rules")
@CrossOrigin(origins = "*")
public class RuleConfigController {

    @Autowired
    private RuleFacadeService ruleFacadeService;

    /**
     * Create a new tax rule.
     * Requires TAX_ADMINISTRATOR role.
     * 
     * @param request Rule creation request containing all rule details
     * @return Created rule with ID and default status (PENDING)
     */
    @PostMapping
    @PreAuthorize("hasRole('TAX_ADMINISTRATOR')")
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody CreateRuleRequest request) {
        RuleResponse response = ruleFacadeService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing tax rule.
     * Validates that rule hasn't been activated (effective date not passed).
     * Requires TAX_ADMINISTRATOR role.
     * 
     * @param ruleId ID of the rule to update
     * @param request Update request with modified fields
     * @return Updated rule response
     */
    @PutMapping("/{ruleId}")
    @PreAuthorize("hasRole('TAX_ADMINISTRATOR')")
    public ResponseEntity<RuleResponse> updateRule(
            @PathVariable String ruleId,
            @Valid @RequestBody UpdateRuleRequest request) {
        RuleResponse response = ruleFacadeService.updateRule(ruleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a pending tax rule.
     * Approver must be different from creator (self-approval prevention).
     * Requires TAX_ADMINISTRATOR role.
     * 
     * @param ruleId ID of the rule to approve
     * @param approverId ID of the user approving the rule
     * @return Approved rule response
     */
    @PostMapping("/{ruleId}/approve")
    @PreAuthorize("hasRole('TAX_ADMINISTRATOR')")
    public ResponseEntity<RuleResponse> approveRule(
            @PathVariable String ruleId,
            @RequestParam String approverId) {
        RuleResponse response = ruleFacadeService.approveRule(ruleId, approverId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a pending tax rule.
     * Requires TAX_ADMINISTRATOR role.
     * 
     * @param ruleId ID of the rule to reject
     * @param reason Reason for rejection
     * @return Rejected rule response
     */
    @PostMapping("/{ruleId}/reject")
    @PreAuthorize("hasRole('TAX_ADMINISTRATOR')")
    public ResponseEntity<RuleResponse> rejectRule(
            @PathVariable String ruleId,
            @RequestParam String reason) {
        RuleResponse response = ruleFacadeService.rejectRule(ruleId, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all rules, optionally filtered by tenant, category, and approval status.
     * 
     * @param tenantId Optional tenant ID filter
     * @param category Optional rule category filter
     * @param status Optional approval status filter
     * @return List of matching rules
     */
    @GetMapping
    public ResponseEntity<List<RuleResponse>> getRules(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        List<RuleResponse> rules = ruleFacadeService.getRules(tenantId, category, status);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get a specific rule by ID.
     * 
     * @param ruleId ID of the rule to retrieve
     * @return Rule details
     */
    @GetMapping("/{ruleId}")
    public ResponseEntity<RuleResponse> getRule(@PathVariable String ruleId) {
        RuleResponse response = ruleFacadeService.getRule(ruleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft delete a rule (sets end date and status to VOIDED).
     * Maintains audit trail by not physically deleting record.
     * Requires TAX_ADMINISTRATOR role.
     * 
     * @param ruleId ID of the rule to void
     * @param reason Reason for voiding the rule
     * @return Success response
     */
    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasRole('TAX_ADMINISTRATOR')")
    public ResponseEntity<Void> voidRule(
            @PathVariable String ruleId,
            @RequestParam String reason) {
        ruleFacadeService.voidRule(ruleId, reason);
        return ResponseEntity.noContent().build();
    }
}
