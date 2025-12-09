package com.munitax.taxengine.controller;

import com.munitax.taxengine.integration.service.RuleServiceIntegration;
import com.munitax.taxengine.model.*;
import com.munitax.taxengine.service.BusinessTaxCalculator;
import com.munitax.taxengine.service.IndividualTaxCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tax-engine")
@Slf4j
public class TaxEngineController {

        private final IndividualTaxCalculator individualCalculator;
        private final BusinessTaxCalculator businessCalculator;
        private final RuleServiceIntegration ruleServiceIntegration;

        @Value("${app.rules.default-tenant-id:dublin}")
        private String defaultTenantId;

        public TaxEngineController(IndividualTaxCalculator individualCalculator,
                        BusinessTaxCalculator businessCalculator,
                        RuleServiceIntegration ruleServiceIntegration) {
                this.individualCalculator = individualCalculator;
                this.businessCalculator = businessCalculator;
                this.ruleServiceIntegration = ruleServiceIntegration;
        }

        @PostMapping("/calculate/individual")
        public TaxCalculationResult calculateIndividual(
                        @RequestBody IndividualCalculationRequest request) {
                
                // Fetch dynamic rules from rule service
                String tenantId = request.tenantId() != null ? request.tenantId() : defaultTenantId;
                int taxYear = request.taxYear() != null ? request.taxYear() : 
                        (request.settings() != null ? request.settings().year() : java.time.Year.now().getValue());
                
                TaxRulesConfig rules = request.rules() != null ? 
                        request.rules() : 
                        ruleServiceIntegration.getIndividualTaxRules(tenantId, taxYear);
                
                log.info("Calculating individual taxes for tenant: {}, year: {} with rules from rule-service", 
                        tenantId, taxYear);
                
                return individualCalculator.calculateTaxes(
                                request.forms(),
                                request.profile(),
                                request.settings(),
                                rules);
        }

        @PostMapping("/calculate/business")
        public NetProfitReturnData calculateBusiness(
                        @RequestBody BusinessCalculationRequest request) {
                
                // Fetch dynamic rules from rule service
                String tenantId = request.tenantId() != null ? request.tenantId() : defaultTenantId;
                int taxYear = request.year();
                
                BusinessTaxRulesConfig rules = request.rules() != null ? 
                        request.rules() : 
                        ruleServiceIntegration.getBusinessTaxRules(tenantId, taxYear);
                
                log.info("Calculating business taxes for tenant: {}, year: {} with rules from rule-service", 
                        tenantId, taxYear);
                
                return businessCalculator.calculateBusinessTax(
                                request.year(),
                                request.estimates(),
                                request.priorCredit(),
                                request.schX(),
                                request.schY(),
                                request.nolCarryforward(),
                                rules);
        }

        public record IndividualCalculationRequest(
                        List<TaxFormData> forms,
                        TaxPayerProfile profile,
                        TaxCalculationResult.TaxReturnSettings settings,
                        TaxRulesConfig rules,
                        String tenantId,
                        Integer taxYear) {
        }

        public record BusinessCalculationRequest(
                        int year,
                        double estimates,
                        double priorCredit,
                        BusinessFederalForm.BusinessScheduleXDetails schX,
                        BusinessFederalForm.BusinessAllocation schY,
                        double nolCarryforward,
                        BusinessTaxRulesConfig rules,
                        String tenantId) {
        }
}
