import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size = 'md', className = '', ...props }, ref) => {
    const baseStyles = 'inline-flex items-center justify-center font-bold rounded-[12px] transition-all duration-150 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-offset-2';
    
    const variantStyles = {
      primary: 'bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white focus:ring-[#970bed] transform hover:scale-[1.01] active:scale-[0.99]',
      secondary: 'bg-[#f0f0f0] hover:bg-[#dcdede] text-[#0f1012] focus:ring-[#babebf] transform hover:scale-[1.01] active:scale-[0.99]',
      outline: 'border border-[#f0f0f0] bg-white hover:bg-[#fbfbfb] text-[#0f1012] focus:ring-[#970bed] transform hover:scale-[1.01] active:scale-[0.99]',
      ghost: 'text-[#5d6567] hover:bg-[#f0f0f0] hover:text-[#0f1012] focus:ring-[#babebf]',
      danger: 'bg-[#ec1656] hover:bg-[#d01149] text-white focus:ring-[#ec1656] transform hover:scale-[1.01] active:scale-[0.99]'
    };
    
    const sizeStyles = {
      sm: 'h-[40px] px-3 text-[12px] leading-[16px]',
      md: 'h-[48px] px-4 text-[14px] leading-[20px]',
      lg: 'h-[52px] px-6 text-[16px] leading-[24px]'
    };
    
    return (
      <button
        ref={ref}
        type="button"
        className={`${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${className}`}
        {...props}
      />
    );
  }
);

Button.displayName = 'Button';
