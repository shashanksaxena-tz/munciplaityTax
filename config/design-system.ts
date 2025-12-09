/**
 * Centralized Design System Configuration
 * Based on Figma E-Filing Design (node-id=57-3884)
 * 
 * This file serves as the single source of truth for all UI styling.
 * Update these values to change the entire application's look and feel.
 */

export const designSystem = {
  // Primary Colors - Brand Identity
  colors: {
    primary: {
      purple: '#970bed',        // Primary brand color (gradient start)
      blue: '#469fe8',          // Primary brand color (gradient end)
      purpleDark: '#7f09c5',    // Hover state for purple
      blueDark: '#3a8bd4',      // Hover state for blue
    },
    
    // Text Colors - Typography Hierarchy
    text: {
      heading: '#0f1012',       // Main headings, primary text
      label: '#102124',         // Form labels, secondary headings
      body: '#5d6567',          // Body text, descriptions
      muted: '#babebf',         // Muted text, placeholders
      disabled: '#dcdede',      // Disabled text
    },
    
    // Background Colors
    background: {
      white: '#ffffff',         // Pure white
      offWhite: '#fbfbfb',      // Cards, sections
      grey: '#f0f0f0',          // Neutral backgrounds
      greyDark: '#0f1012',      // Dark backgrounds, footer
    },
    
    // Border Colors
    border: {
      default: '#dcdede',       // Universal border color
      light: '#f0f0f0',         // Lighter borders
      focus: '#970bed',         // Focus state borders
    },
    
    // Status Colors
    status: {
      success: '#10b981',       // Success states, positive indicators
      error: '#ec1656',         // Error states, negative indicators
      warning: '#f59e0b',       // Warning states, pending indicators
      info: '#469fe8',          // Info states, neutral indicators
    },
    
    // Tinted Backgrounds
    tints: {
      blueLightBg: '#ebf4ff',   // Light blue backgrounds
      blueAltBg: '#dfedff',     // Alternative blue backgrounds
      greenBg: '#d5faeb',       // Success/green tinted backgrounds
      purpleBg: '#970bed',      // Purple tinted backgrounds (10% opacity recommended)
      warningBg: '#f59e0b',     // Warning tinted backgrounds (10% opacity recommended)
      errorBg: '#ec1656',       // Error tinted backgrounds (10% opacity recommended)
    },
  },
  
  // Gradients
  gradients: {
    primary: {
      from: '#970bed',
      to: '#469fe8',
      hover: {
        from: '#7f09c5',
        to: '#3a8bd4',
      },
    },
    success: {
      from: '#10b981',
      to: '#10b981',
      hover: {
        from: '#059669',
        to: '#059669',
      },
    },
  },
  
  // Typography
  typography: {
    fontFamily: {
      sans: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      mono: 'ui-monospace, SFMono-Regular, "SF Mono", Menlo, Monaco, monospace',
    },
    fontSize: {
      xs: '0.75rem',      // 12px
      sm: '0.875rem',     // 14px
      base: '1rem',       // 16px
      lg: '1.125rem',     // 18px
      xl: '1.25rem',      // 20px
      '2xl': '1.5rem',    // 24px
      '3xl': '1.875rem',  // 30px
      '4xl': '2.25rem',   // 36px
    },
    fontWeight: {
      normal: '400',
      medium: '500',
      semibold: '600',
      bold: '700',
      black: '900',
    },
  },
  
  // Spacing
  spacing: {
    xs: '0.25rem',    // 4px
    sm: '0.5rem',     // 8px
    md: '0.75rem',    // 12px
    lg: '1rem',       // 16px
    xl: '1.5rem',     // 24px
    '2xl': '2rem',    // 32px
    '3xl': '3rem',    // 48px
    '4xl': '4rem',    // 64px
  },
  
  // Border Radius
  borderRadius: {
    sm: '0.25rem',    // 4px
    md: '0.5rem',     // 8px
    lg: '0.75rem',    // 12px
    xl: '1rem',       // 16px
    '2xl': '1.5rem',  // 24px
    full: '9999px',   // Fully rounded
  },
  
  // Shadows
  shadows: {
    sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
    md: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
    lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1)',
    primary: '0 4px 12px rgba(151, 11, 237, 0.2)',  // Purple shadow
    success: '0 4px 12px rgba(16, 185, 129, 0.2)',  // Green shadow
  },
  
  // Form Elements
  forms: {
    input: {
      height: '48px',
      borderColor: '#dcdede',
      focusBorderColor: '#970bed',
      focusRing: 'rgba(151, 11, 237, 0.2)',
      borderRadius: '0.75rem',  // 12px
    },
    button: {
      height: '48px',
      borderRadius: '0.75rem',  // 12px
      paddingX: '1.5rem',       // 24px
      paddingY: '0.75rem',      // 12px
    },
  },
  
  // Icons
  icons: {
    size: {
      sm: '1rem',      // 16px
      md: '1.25rem',   // 20px
      lg: '1.5rem',    // 24px
      xl: '2rem',      // 32px
    },
  },
  
  // Transitions
  transitions: {
    fast: '150ms',
    normal: '200ms',
    slow: '300ms',
  },
  
  // Z-Index Layers
  zIndex: {
    dropdown: 1000,
    sticky: 1020,
    fixed: 1030,
    modalBackdrop: 1040,
    modal: 1050,
    popover: 1060,
    tooltip: 1070,
  },
};

// Utility function to generate Tailwind classes from design system
export const tw = {
  // Gradient button (primary)
  buttonPrimary: `bg-gradient-to-r from-[${designSystem.colors.primary.purple}] to-[${designSystem.colors.primary.blue}] hover:from-[${designSystem.colors.primary.purpleDark}] hover:to-[${designSystem.colors.primary.blueDark}] text-white rounded-xl font-bold shadow-lg shadow-[${designSystem.colors.primary.purple}]/20 transition-all`,
  
  // Success button
  buttonSuccess: `bg-gradient-to-r from-[${designSystem.colors.status.success}] to-[${designSystem.colors.status.success}] hover:from-[#059669] hover:to-[#059669] text-white rounded-xl font-bold shadow-lg shadow-[${designSystem.colors.status.success}]/20 transition-all`,
  
  // Secondary button
  buttonSecondary: `border border-[${designSystem.colors.border.default}] text-[${designSystem.colors.text.body}] hover:bg-[${designSystem.colors.background.offWhite}] rounded-xl font-medium transition-all`,
  
  // Input field
  input: `w-full px-4 py-3 border border-[${designSystem.colors.border.default}] focus:border-[${designSystem.colors.border.focus}] focus:ring-[${designSystem.colors.border.focus}]/20 focus:ring-2 rounded-xl outline-none transition-all`,
  
  // Card
  card: `bg-white rounded-2xl border border-[${designSystem.colors.border.default}] shadow-sm p-6`,
  
  // Card header
  cardHeader: `bg-[${designSystem.colors.background.offWhite}] px-6 py-4 border-b border-[${designSystem.colors.border.default}]`,
  
  // Table header
  tableHeader: `bg-[${designSystem.colors.background.offWhite}] text-[${designSystem.colors.text.body}] uppercase text-xs font-medium tracking-wider`,
  
  // Table row
  tableRow: `hover:bg-[${designSystem.colors.background.offWhite}] transition-colors border-b border-[${designSystem.colors.border.default}]`,
  
  // Badge success
  badgeSuccess: `bg-[${designSystem.colors.tints.greenBg}] text-[${designSystem.colors.status.success}] px-3 py-1 rounded-full text-xs font-medium`,
  
  // Badge error
  badgeError: `bg-[${designSystem.colors.status.error}]/10 text-[${designSystem.colors.status.error}] px-3 py-1 rounded-full text-xs font-medium`,
  
  // Badge warning
  badgeWarning: `bg-[${designSystem.colors.status.warning}]/10 text-[${designSystem.colors.status.warning}] px-3 py-1 rounded-full text-xs font-medium`,
  
  // Badge info
  badgeInfo: `bg-[${designSystem.colors.tints.blueLightBg}] text-[${designSystem.colors.primary.blue}] px-3 py-1 rounded-full text-xs font-medium`,
  
  // Progress bar
  progressBar: `h-2 rounded-full bg-gradient-to-r from-[${designSystem.colors.primary.purple}] to-[${designSystem.colors.primary.blue}]`,
  
  // Progress bar background
  progressBg: `h-2 rounded-full bg-[${designSystem.colors.border.default}]`,
};

// Export individual color palettes for direct use
export const colors = designSystem.colors;
export const gradients = designSystem.gradients;
export const typography = designSystem.typography;
export const spacing = designSystem.spacing;
export const borderRadius = designSystem.borderRadius;

export default designSystem;
