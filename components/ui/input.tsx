import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, className = '', required, ...props }, ref) => {
    return (
      <div className="space-y-2 w-full">
        {label && (
          <label className="flex gap-1 text-[14px] leading-[20px] font-medium text-[#102124]">
            <span>{label}</span>
            {required && <span className="text-[#ec1656]">*</span>}
          </label>
        )}
        <input
          ref={ref}
          className={`w-full h-[48px] px-3 py-3 bg-white border rounded-[8px] text-[14px] leading-[20px] font-medium text-[#0f1012] placeholder-[#5d6567] transition duration-150 focus:outline-none focus:ring-2 focus:ring-[#970bed] focus:border-transparent ${
            error ? 'border-[#ec1656]' : 'border-[#dcdede]'
          } ${className}`}
          {...props}
        />
        {error && (
          <p className="text-[12px] text-[#ec1656] font-medium">{error}</p>
        )}
        {helperText && !error && (
          <p className="text-[12px] text-[#5d6567]">{helperText}</p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
