import React, { useState } from 'react';
import { HelpCircle } from 'lucide-react';

interface TooltipProps {
  content: string;
  children?: React.ReactNode;
}

export const Tooltip: React.FC<TooltipProps> = ({ content, children }) => {
  const [isVisible, setIsVisible] = useState(false);

  return (
    <div 
      className="relative inline-flex items-center"
      onMouseEnter={() => setIsVisible(true)}
      onMouseLeave={() => setIsVisible(false)}
    >
      {children ? children : <HelpCircle className="w-4 h-4 text-[#babebf] hover:text-[#970bed] transition-colors cursor-help" />}
      
      {isVisible && (
        <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-3 py-2 bg-[#0f1012] text-white text-xs rounded-lg shadow-xl w-48 z-50 animate-fadeIn">
          {content}
          <div className="absolute top-full left-1/2 transform -translate-x-1/2 border-4 border-transparent border-t-[#0f1012]"></div>
        </div>
      )}
    </div>
  );
};