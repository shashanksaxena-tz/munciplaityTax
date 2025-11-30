package com.munitax.taxengine.dto;

import java.util.Map;

/**
 * Response DTO for auto-calculation endpoint
 */
public record ScheduleXAutoCalcResponse(
    Double calculatedValue,
    String explanation,
    Map<String, Object> details
) {}
