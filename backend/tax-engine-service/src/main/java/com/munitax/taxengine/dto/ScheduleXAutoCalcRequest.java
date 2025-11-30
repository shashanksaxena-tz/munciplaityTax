package com.munitax.taxengine.dto;

import java.util.Map;

/**
 * Request DTO for auto-calculation endpoint
 */
public record ScheduleXAutoCalcRequest(
    String fieldName,
    Map<String, Double> inputs
) {}
