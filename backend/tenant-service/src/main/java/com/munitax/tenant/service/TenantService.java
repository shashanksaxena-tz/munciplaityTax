package com.munitax.tenant.service;

import com.munitax.tenant.exception.TenantNotFoundException;
import com.munitax.tenant.model.Tenant;
import com.munitax.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing tenant CRUD operations.
 */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public Optional<Tenant> getTenantById(String tenantId) {
        return tenantRepository.findById(tenantId);
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        if (tenant.getTenantId() == null || tenant.getTenantId().isBlank()) {
            // Generate tenant ID from name
            tenant.setTenantId(generateTenantId(tenant.getName()));
        }
        if (tenant.getSchemaName() == null || tenant.getSchemaName().isBlank()) {
            tenant.setSchemaName("tenant_" + tenant.getTenantId().replace("-", "_"));
        }
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant updateTenant(String tenantId, Tenant updatedTenant) {
        return tenantRepository.findById(tenantId)
                .map(existing -> {
                    existing.setName(updatedTenant.getName());
                    if (updatedTenant.getDbUrl() != null) {
                        existing.setDbUrl(updatedTenant.getDbUrl());
                    }
                    if (updatedTenant.getDbUsername() != null) {
                        existing.setDbUsername(updatedTenant.getDbUsername());
                    }
                    if (updatedTenant.getDbPassword() != null) {
                        existing.setDbPassword(updatedTenant.getDbPassword());
                    }
                    return tenantRepository.save(existing);
                })
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    @Transactional
    public void deleteTenant(String tenantId) {
        if (tenantRepository.existsById(tenantId)) {
            tenantRepository.deleteById(tenantId);
        } else {
            throw new TenantNotFoundException(tenantId);
        }
    }

    private String generateTenantId(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
