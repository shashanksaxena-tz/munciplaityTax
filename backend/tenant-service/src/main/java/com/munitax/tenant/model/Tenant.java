package com.munitax.tenant.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenants", schema = "public") // Tenants are stored in the default public schema
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tenant {
    @Id
    private String tenantId;
    private String name;
    private String schemaName;
    private String dbUrl; // Optional: if using separate DBs
    private String dbUsername;
    private String dbPassword;
}
