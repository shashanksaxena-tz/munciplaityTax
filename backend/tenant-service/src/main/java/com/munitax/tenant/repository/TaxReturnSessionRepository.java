package com.munitax.tenant.repository;

import com.munitax.tenant.model.TaxReturnSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxReturnSessionRepository extends JpaRepository<TaxReturnSession, String> {
    
    List<TaxReturnSession> findByTenantIdAndUserId(String tenantId, String userId);
    
    List<TaxReturnSession> findByTenantIdAndUserIdAndType(String tenantId, String userId, TaxReturnSession.SessionType type);
    
    List<TaxReturnSession> findByTenantIdAndUserIdAndStatus(String tenantId, String userId, TaxReturnSession.SessionStatus status);
}
