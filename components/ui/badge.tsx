import React from 'react';

export const Badge = React.forwardRef<HTMLSpanElement, React.HTMLAttributes<HTMLSpanElement> & { variant?: 'default' | 'success' | 'error' | 'warning' | 'info' }>(
  ({ className = '', variant = 'default', ...props }, ref) => {
    const variantStyles = {
      default: 'bg-[#f0f0f0] text-[#5d6567]',
      success: 'bg-[#d5faeb] text-[#10b981]',
      error: 'bg-[#ec1656]/10 text-[#ec1656]',
      warning: 'bg-[#f59e0b]/10 text-[#f59e0b]',
      info: 'bg-[#ebf4ff] text-[#469fe8]',
    };
    
    return (
      <span 
        ref={ref} 
        className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium ${variantStyles[variant]} ${className}`}
        {...props} 
      />
    );
  }
);
Badge.displayName = 'Badge';
