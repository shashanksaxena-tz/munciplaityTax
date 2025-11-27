package com.munitax.tenant.service;

import com.munitax.tenant.model.TaxReturnSession;
import com.munitax.tenant.repository.TaxReturnSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SessionService {

    private final TaxReturnSessionRepository sessionRepository;

    public SessionService(TaxReturnSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public TaxReturnSession createSession(TaxReturnSession session) {
        session.setCreatedDate(LocalDateTime.now());
        session.setLastModifiedDate(LocalDateTime.now());
        if (session.getStatus() == null) {
            session.setStatus(TaxReturnSession.SessionStatus.DRAFT);
        }
        return sessionRepository.save(session);
    }

    public List<TaxReturnSession> getAllSessions(String tenantId, String userId) {
        return sessionRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    public List<TaxReturnSession> getSessionsByType(String tenantId, String userId, TaxReturnSession.SessionType type) {
        return sessionRepository.findByTenantIdAndUserIdAndType(tenantId, userId, type);
    }

    public List<TaxReturnSession> getSessionsByStatus(String tenantId, String userId, TaxReturnSession.SessionStatus status) {
        return sessionRepository.findByTenantIdAndUserIdAndStatus(tenantId, userId, status);
    }

    public Optional<TaxReturnSession> getSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    @Transactional
    public TaxReturnSession updateSession(String sessionId, TaxReturnSession updatedSession) {
        return sessionRepository.findById(sessionId)
                .map(existing -> {
                    existing.setProfileJson(updatedSession.getProfileJson());
                    existing.setSettingsJson(updatedSession.getSettingsJson());
                    existing.setFormsJson(updatedSession.getFormsJson());
                    existing.setCalculationResultJson(updatedSession.getCalculationResultJson());
                    existing.setBusinessFilingsJson(updatedSession.getBusinessFilingsJson());
                    existing.setNetProfitFilingsJson(updatedSession.getNetProfitFilingsJson());
                    existing.setReconciliationsJson(updatedSession.getReconciliationsJson());
                    existing.setStatus(updatedSession.getStatus());
                    existing.setNotes(updatedSession.getNotes());
                    existing.setLastModifiedDate(LocalDateTime.now());
                    
                    if (updatedSession.getStatus() == TaxReturnSession.SessionStatus.SUBMITTED && existing.getSubmittedDate() == null) {
                        existing.setSubmittedDate(LocalDateTime.now());
                    }
                    
                    return sessionRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }

    @Transactional
    public void deleteSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    @Transactional
    public void deleteAllSessions(String tenantId, String userId) {
        List<TaxReturnSession> sessions = sessionRepository.findByTenantIdAndUserId(tenantId, userId);
        sessionRepository.deleteAll(sessions);
    }
}
