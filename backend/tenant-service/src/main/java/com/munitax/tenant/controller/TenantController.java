package com.munitax.tenant.controller;

import com.munitax.tenant.model.Tenant;
import com.munitax.tenant.service.TenantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for tenant management operations.
 * Provides CRUD endpoints for onboarding and managing tenants.
 */
@RestController
@RequestMapping("/api/v1/tenants")
@CrossOrigin(origins = "*")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Get all tenants.
     * Accessible only to admin users.
     */
    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    /**
     * Get a specific tenant by ID.
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<Tenant> getTenant(@PathVariable String tenantId) {
        return tenantService.getTenantById(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create (onboard) a new tenant.
     * Accessible only to admin users.
     */
    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody CreateTenantRequest request) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(request.tenantId());
        tenant.setName(request.name());
        tenant.setSchemaName(request.schemaName());
        tenant.setDbUrl(request.dbUrl());
        tenant.setDbUsername(request.dbUsername());
        tenant.setDbPassword(request.dbPassword());
        
        Tenant created = tenantService.createTenant(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing tenant's configuration.
     * Accessible only to admin users.
     */
    @PutMapping("/{tenantId}")
    public ResponseEntity<Tenant> updateTenant(
            @PathVariable String tenantId,
            @RequestBody UpdateTenantRequest request) {
        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setDbUrl(request.dbUrl());
        tenant.setDbUsername(request.dbUsername());
        tenant.setDbPassword(request.dbPassword());
        
        try {
            Tenant updated = tenantService.updateTenant(tenantId, tenant);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a tenant.
     * Accessible only to admin users.
     */
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@PathVariable String tenantId) {
        try {
            tenantService.deleteTenant(tenantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DTO Records
    public record CreateTenantRequest(
            String tenantId,
            String name,
            String schemaName,
            String dbUrl,
            String dbUsername,
            String dbPassword
    ) {}

    public record UpdateTenantRequest(
            String name,
            String dbUrl,
            String dbUsername,
            String dbPassword
    ) {}
}
