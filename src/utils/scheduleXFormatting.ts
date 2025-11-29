/**
 * Schedule X Formatting Utilities (T013)
 * 
 * Provides formatting functions for currency, percentages, and other display values
 * in Schedule X components.
 */

/**
 * Format number as US currency (e.g., $1,234.56)
 * 
 * @param amount Numeric amount to format
 * @param includeSign Include + or - sign (default false)
 * @returns Formatted currency string
 */
export function formatCurrency(amount: number | null | undefined, includeSign: boolean = false): string {
  if (amount === null || amount === undefined) {
    return '$0.00';
  }
  
  const formatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  
  const formatted = formatter.format(Math.abs(amount));
  
  if (includeSign) {
    if (amount > 0) {
      return `+${formatted}`;
    } else if (amount < 0) {
      return `-${formatted}`;
    }
  } else if (amount < 0) {
    return `-${formatted}`;
  }
  
  return formatted;
}

/**
 * Format number as percentage (e.g., 25.5%)
 * 
 * @param value Decimal value (e.g., 0.255 for 25.5%)
 * @param decimals Number of decimal places (default 1)
 * @returns Formatted percentage string
 */
export function formatPercentage(value: number | null | undefined, decimals: number = 1): string {
  if (value === null || value === undefined) {
    return '0.0%';
  }
  
  const pct = value * 100;
  return `${pct.toFixed(decimals)}%`;
}

/**
 * Format number with comma separators (e.g., 1,234.56)
 * 
 * @param value Numeric value
 * @param decimals Number of decimal places (default 2)
 * @returns Formatted number string
 */
export function formatNumber(value: number | null | undefined, decimals: number = 2): string {
  if (value === null || value === undefined) {
    return '0';
  }
  
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
}

/**
 * Parse currency string to number (strips $, commas)
 * 
 * @param currencyString Currency string (e.g., "$1,234.56" or "1,234.56")
 * @returns Numeric value
 */
export function parseCurrency(currencyString: string): number {
  if (!currencyString) {
    return 0;
  }
  
  // Remove $, commas, and spaces
  const cleaned = currencyString.replace(/[$,\s]/g, '');
  const parsed = parseFloat(cleaned);
  
  return isNaN(parsed) ? 0 : parsed;
}

/**
 * Format confidence score as percentage with color coding
 * 
 * @param confidence Confidence score (0.0 to 1.0)
 * @returns Object with formatted percentage and color class
 */
export function formatConfidenceScore(confidence: number | null | undefined): {
  formatted: string;
  colorClass: string;
  badgeVariant: 'success' | 'warning' | 'danger';
} {
  if (confidence === null || confidence === undefined) {
    return {
      formatted: 'N/A',
      colorClass: 'text-gray-500',
      badgeVariant: 'warning'
    };
  }
  
  const pct = confidence * 100;
  
  // Green: ≥90%, Yellow: 70-89%, Red: <70%
  if (confidence >= 0.9) {
    return {
      formatted: `${pct.toFixed(0)}%`,
      colorClass: 'text-green-600',
      badgeVariant: 'success'
    };
  } else if (confidence >= 0.7) {
    return {
      formatted: `${pct.toFixed(0)}%`,
      colorClass: 'text-yellow-600',
      badgeVariant: 'warning'
    };
  } else {
    return {
      formatted: `${pct.toFixed(0)}%`,
      colorClass: 'text-red-600',
      badgeVariant: 'danger'
    };
  }
}

/**
 * Abbreviate large numbers (e.g., 1.2M, 500K)
 * 
 * @param value Numeric value
 * @returns Abbreviated string
 */
export function abbreviateNumber(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '0';
  }
  
  const absValue = Math.abs(value);
  const sign = value < 0 ? '-' : '';
  
  if (absValue >= 1_000_000) {
    return `${sign}${(absValue / 1_000_000).toFixed(1)}M`;
  } else if (absValue >= 1_000) {
    return `${sign}${(absValue / 1_000).toFixed(1)}K`;
  } else {
    return `${sign}${absValue.toFixed(0)}`;
  }
}

/**
 * Format ISO timestamp to readable date/time
 * 
 * @param isoTimestamp ISO 8601 timestamp string
 * @returns Formatted date/time string
 */
export function formatTimestamp(isoTimestamp: string | null | undefined): string {
  if (!isoTimestamp) {
    return 'N/A';
  }
  
  try {
    const date = new Date(isoTimestamp);
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  } catch {
    return 'Invalid date';
  }
}

/**
 * Truncate long text with ellipsis
 * 
 * @param text Text to truncate
 * @param maxLength Maximum length before truncation
 * @returns Truncated text
 */
export function truncateText(text: string | null | undefined, maxLength: number = 50): string {
  if (!text) {
    return '';
  }
  
  if (text.length <= maxLength) {
    return text;
  }
  
  return `${text.substring(0, maxLength)}...`;
}
