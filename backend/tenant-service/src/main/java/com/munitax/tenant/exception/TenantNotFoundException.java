package com.munitax.tenant.exception;

/**
 * Exception thrown when a tenant is not found.
 */
public class TenantNotFoundException extends RuntimeException {
    
    private final String tenantId;
    
    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
        this.tenantId = tenantId;
    }
    
    public TenantNotFoundException(String tenantId, Throwable cause) {
        super("Tenant not found: " + tenantId, cause);
        this.tenantId = tenantId;
    }
    
    public String getTenantId() {
        return tenantId;
    }
}
