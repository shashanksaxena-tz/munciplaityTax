/**
 * Formatting utilities for displaying numbers, currency, dates, etc.
 */

/**
 * Format a number as currency (USD).
 */
export const formatCurrency = (value: number | undefined | null): string => {
  if (value === undefined || value === null) {
    return '$0.00';
  }
  
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
};

/**
 * Format a number as a percentage.
 */
export const formatPercentage = (value: number | undefined | null, decimals = 2): string => {
  if (value === undefined || value === null) {
    return '0%';
  }
  
  return `${value.toFixed(decimals)}%`;
};

/**
 * Format a date string to a readable format.
 */
export const formatDate = (dateString: string | undefined | null): string => {
  if (!dateString) {
    return '';
  }
  
  try {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }).format(date);
  } catch {
    return dateString;
  }
};

/**
 * Format a number with thousand separators.
 */
export const formatNumber = (value: number | undefined | null, decimals = 0): string => {
  if (value === undefined || value === null) {
    return '0';
  }
  
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
};
