package com.munitax.taxengine.controller;

import com.munitax.taxengine.model.*;
import com.munitax.taxengine.service.BusinessTaxCalculator;
import com.munitax.taxengine.service.IndividualTaxCalculator;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tax-engine")
public class TaxEngineController {

        private final IndividualTaxCalculator individualCalculator;
        private final BusinessTaxCalculator businessCalculator;

        public TaxEngineController(IndividualTaxCalculator individualCalculator,
                        BusinessTaxCalculator businessCalculator) {
                this.individualCalculator = individualCalculator;
                this.businessCalculator = businessCalculator;
        }

        @PostMapping("/calculate/individual")
        public TaxCalculationResult calculateIndividual(
                        @RequestBody IndividualCalculationRequest request) {
                return individualCalculator.calculateTaxes(
                                request.forms(),
                                request.profile(),
                                request.settings(),
                                request.rules());
        }

        @PostMapping("/calculate/business")
        public NetProfitReturnData calculateBusiness(
                        @RequestBody BusinessCalculationRequest request) {
                return businessCalculator.calculateBusinessTax(
                                request.year(),
                                request.estimates(),
                                request.priorCredit(),
                                request.schX(),
                                request.schY(),
                                request.nolCarryforward(),
                                request.rules());
        }

        public record IndividualCalculationRequest(
                        List<TaxFormData> forms,
                        TaxPayerProfile profile,
                        TaxCalculationResult.TaxReturnSettings settings,
                        TaxRulesConfig rules) {
        }

        public record BusinessCalculationRequest(
                        int year,
                        double estimates,
                        double priorCredit,
                        BusinessFederalForm.BusinessScheduleXDetails schX,
                        BusinessFederalForm.BusinessAllocation schY,
                        double nolCarryforward,
                        BusinessTaxRulesConfig rules) {
        }
}
