package com.munitax.taxengine.dto;

import java.util.List;

/**
 * Response DTO for multi-year comparison endpoint
 */
public record MultiYearComparisonDto(
    List<Integer> years,
    List<BusinessScheduleXDetailsDto> scheduleXData
) {}
