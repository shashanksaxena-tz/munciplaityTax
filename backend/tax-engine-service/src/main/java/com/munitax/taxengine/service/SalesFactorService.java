package com.munitax.taxengine.service;

import com.munitax.taxengine.domain.apportionment.SaleTransaction;
import com.munitax.taxengine.domain.apportionment.SalesFactor;
import com.munitax.taxengine.domain.apportionment.SourcingMethodElection;
import com.munitax.taxengine.repository.SaleTransactionRepository;
import com.munitax.taxengine.repository.SalesFactorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for calculating sales factor with sourcing method election.
 * Handles Finnigan/Joyce election and throwback adjustments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesFactorService {

    private final SalesFactorRepository salesFactorRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final SourcingService sourcingService;
    private final ThrowbackService throwbackService;
    private final NexusService nexusService;

    private static final int SCALE = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * Calculate sales factor percentage with sourcing method election.
     *
     * @param scheduleYId            the Schedule Y ID
     * @param affiliatedSales        map of entity ID to total sales
     * @param nexusStatus            map of entity ID to nexus status
     * @param sourcingMethodElection the sourcing method election
     * @param tenantId               the tenant ID for multi-tenant isolation
     * @return sales factor percentage (0-100)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateSalesFactorPercentage(UUID scheduleYId,
                                                     Map<String, BigDecimal> affiliatedSales,
                                                     Map<String, Boolean> nexusStatus,
                                                     SourcingMethodElection sourcingMethodElection,
                                                     UUID tenantId) {
        log.debug("Calculating sales factor for Schedule Y: {}, sourcing method: {}",
                scheduleYId, sourcingMethodElection);

        // Get sales factor entity
        SalesFactor salesFactor = salesFactorRepository.findByScheduleYId(scheduleYId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sales factor not found for Schedule Y: " + scheduleYId));

        // Calculate denominator based on sourcing method election
        BigDecimal denominator = sourcingService.calculateSalesDenominator(
                affiliatedSales, nexusStatus, sourcingMethodElection, tenantId);

        // Ohio sales (numerator) includes throwback adjustments
        BigDecimal numerator = salesFactor.getOhioSales().add(salesFactor.getThrowbackAdjustment());

        // Calculate percentage
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Sales factor denominator is zero for Schedule Y: {}", scheduleYId);
            return BigDecimal.ZERO;
        }

        BigDecimal percentage = numerator
                .divide(denominator, SCALE + 2, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(SCALE, RoundingMode.HALF_UP);

        log.info("Sales factor calculated: {}% (numerator: {}, denominator: {})",
                percentage, numerator, denominator);

        return percentage;
    }

    /**
     * Calculate total throwback adjustment for a sales factor.
     * Sums throwback amounts from all transactions where throwback was applied.
     *
     * @param salesFactorId the sales factor ID
     * @return total throwback adjustment amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateThrowbackAdjustment(UUID salesFactorId) {
        log.debug("Calculating throwback adjustment for sales factor: {}", salesFactorId);

        BigDecimal adjustment = saleTransactionRepository.sumThrowbackAdjustments(salesFactorId);

        log.info("Total throwback adjustment: {}", adjustment);
        return adjustment;
    }

    /**
     * Apply sourcing and throwback rules to all sale transactions.
     * Task: T076 [US2] - Updated to integrate throwback adjustments
     *
     * @param salesFactorId          the sales factor ID
     * @param sourcingMethodElection the sourcing method election
     * @param throwbackElection      the throwback election (THROWBACK or THROWOUT)
     * @param businessId             the business ID
     * @param tenantId               the tenant ID for multi-tenant isolation
     * @return updated sales factor with sourcing applied
     */
    @Transactional
    public SalesFactor applySourcingRules(UUID salesFactorId,
                                         SourcingMethodElection sourcingMethodElection,
                                         com.munitax.taxengine.domain.apportionment.ThrowbackElection throwbackElection,
                                         UUID businessId,
                                         UUID tenantId) {
        log.info("Applying sourcing rules to sales factor: {}, sourcing method: {}, throwback: {}",
                salesFactorId, sourcingMethodElection, throwbackElection);

        SalesFactor salesFactor = salesFactorRepository.findById(salesFactorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sales factor not found: " + salesFactorId));

        // Get all transactions for this sales factor
        List<SaleTransaction> transactions = saleTransactionRepository.findBySalesFactorId(salesFactorId);

        BigDecimal totalOhioSourcedAmount = BigDecimal.ZERO;
        BigDecimal totalThrowbackAmount = BigDecimal.ZERO;
        int throwbackCount = 0;

        // Apply sourcing to each transaction
        for (SaleTransaction transaction : transactions) {
            // Determine if destination state has nexus
            boolean hasNexus = nexusService.hasNexus(businessId, transaction.getDestinationState(), tenantId);

            if (!hasNexus) {
                // Apply throwback rule with election
                BigDecimal adjustedAmount = throwbackService.applyThrowbackRule(
                        transaction.getAmount(),
                        transaction.getOriginState(),
                        transaction.getDestinationState(),
                        businessId,
                        tenantId,
                        throwbackElection
                );

                transaction.setThrowbackApplied(true);
                
                if (throwbackElection == com.munitax.taxengine.domain.apportionment.ThrowbackElection.THROWBACK) {
                    // Throwback: Add to origin state (Ohio) numerator
                    transaction.setThrowbackAmount(adjustedAmount);
                    transaction.setOhioSourcedAmount(adjustedAmount);
                    totalThrowbackAmount = totalThrowbackAmount.add(adjustedAmount);
                } else {
                    // Throwout: Exclude from both numerator and denominator
                    transaction.setThrowbackAmount(BigDecimal.ZERO);
                    transaction.setOhioSourcedAmount(BigDecimal.ZERO);
                }
                
                throwbackCount++;
            } else if (transaction.getDestinationState().equals("OH")) {
                // Sale to Ohio (has nexus by definition)
                transaction.setThrowbackApplied(false);
                transaction.setThrowbackAmount(BigDecimal.ZERO);
                transaction.setOhioSourcedAmount(transaction.getAmount());
            } else {
                // Sale to state with nexus (not Ohio)
                transaction.setThrowbackApplied(false);
                transaction.setThrowbackAmount(BigDecimal.ZERO);
                transaction.setOhioSourcedAmount(BigDecimal.ZERO);
            }

            totalOhioSourcedAmount = totalOhioSourcedAmount.add(transaction.getOhioSourcedAmount());

            saleTransactionRepository.save(transaction);
        }

        // Update sales factor with calculated amounts
        salesFactor.setThrowbackAdjustment(totalThrowbackAmount);

        // Calculate and set sales factor percentage
        BigDecimal percentage = calculateSalesFactorPercentage(
                salesFactor.getScheduleY().getScheduleYId(),
                Map.of(businessId.toString(), salesFactor.getTotalSalesEverywhere()),
                Map.of(businessId.toString(), true),
                sourcingMethodElection,
                tenantId
        );

        salesFactor.setSalesFactorPercentage(percentage);

        log.info("Sourcing rules applied: Ohio sourced={}, Throwback={}, Throwback count={}",
                totalOhioSourcedAmount, totalThrowbackAmount, throwbackCount);

        return salesFactorRepository.save(salesFactor);
    }

    /**
     * Calculate service revenue portion of total sales.
     *
     * @param salesFactorId the sales factor ID
     * @return total service revenue amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateServiceRevenue(UUID salesFactorId) {
        log.debug("Calculating service revenue for sales factor: {}", salesFactorId);

        List<SaleTransaction> serviceTransactions = saleTransactionRepository
                .findServiceTransactions(salesFactorId);

        BigDecimal serviceRevenue = serviceTransactions.stream()
                .map(SaleTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Service revenue calculated: {}", serviceRevenue);
        return serviceRevenue;
    }

    /**
     * Calculate tangible goods sales portion of total sales.
     *
     * @param salesFactorId the sales factor ID
     * @return total tangible goods sales amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTangibleGoodsSales(UUID salesFactorId) {
        log.debug("Calculating tangible goods sales for sales factor: {}", salesFactorId);

        long tangibleGoodsCount = saleTransactionRepository.countBySaleType(salesFactorId,
                com.munitax.taxengine.domain.apportionment.SaleType.TANGIBLE_GOODS);

        // This would be enhanced to sum actual amounts
        log.info("Tangible goods transaction count: {}", tangibleGoodsCount);

        // For now, return placeholder
        return BigDecimal.ZERO;
    }
}
