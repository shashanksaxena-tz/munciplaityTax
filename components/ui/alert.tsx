import React from 'react';

export const Alert = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement> & { variant?: 'default' | 'success' | 'error' | 'warning' }>(
  ({ className = '', variant = 'default', ...props }, ref) => {
    const variantStyles = {
      default: 'bg-[#ebf4ff] border-[#469fe8]/20 text-[#0f1012]',
      success: 'bg-[#d5faeb] border-[#10b981]/20 text-[#0f1012]',
      error: 'bg-[#ec1656]/10 border-[#ec1656]/20 text-[#0f1012]',
      warning: 'bg-[#f59e0b]/10 border-[#f59e0b]/20 text-[#0f1012]',
    };
    
    return (
      <div 
        ref={ref} 
        role="alert" 
        className={`border rounded-xl p-4 ${variantStyles[variant]} ${className}`}
        {...props} 
      />
    );
  }
);
Alert.displayName = 'Alert';

export const AlertDescription = React.forwardRef<HTMLParagraphElement, React.HTMLAttributes<HTMLParagraphElement>>(
  ({ className = '', ...props }, ref) => (
    <div ref={ref} className={`text-sm text-[#5d6567] ${className}`} {...props} />
  )
);
AlertDescription.displayName = 'AlertDescription';
