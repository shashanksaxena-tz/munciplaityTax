/**
 * CollapsibleAccordion Component (T014 - Reusable UI Component)
 * 
 * A reusable collapsible accordion component for organizing content in expandable sections.
 * Used by ScheduleXAccordion for Add-Backs vs Deductions sections.
 */

import React, { useState } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';

export interface AccordionSection {
  id: string;
  title: string;
  subtitle?: string;
  badge?: React.ReactNode;
  content: React.ReactNode;
  defaultExpanded?: boolean;
}

export interface CollapsibleAccordionProps {
  sections: AccordionSection[];
  className?: string;
  allowMultipleExpanded?: boolean;
}

export const CollapsibleAccordion: React.FC<CollapsibleAccordionProps> = ({
  sections,
  className = '',
  allowMultipleExpanded = true,
}) => {
  // Track which sections are expanded
  const [expandedSections, setExpandedSections] = useState<Set<string>>(
    new Set(sections.filter(s => s.defaultExpanded).map(s => s.id))
  );

  const toggleSection = (sectionId: string) => {
    setExpandedSections(prev => {
      const newExpanded = new Set(prev);
      
      if (newExpanded.has(sectionId)) {
        newExpanded.delete(sectionId);
      } else {
        if (!allowMultipleExpanded) {
          // Collapse all other sections if only one can be expanded at a time
          newExpanded.clear();
        }
        newExpanded.add(sectionId);
      }
      
      return newExpanded;
    });
  };

  return (
    <div className={`space-y-2 ${className}`}>
      {sections.map(section => {
        const isExpanded = expandedSections.has(section.id);
        
        return (
          <div
            key={section.id}
            className="border border-gray-200 rounded-lg overflow-hidden shadow-sm"
          >
            {/* Accordion Header */}
            <button
              onClick={() => toggleSection(section.id)}
              className="w-full px-4 py-3 bg-gray-50 hover:bg-gray-100 transition-colors 
                       flex items-center justify-between text-left"
              aria-expanded={isExpanded}
              aria-controls={`accordion-content-${section.id}`}
            >
              <div className="flex items-center gap-3 flex-1">
                <div className="flex-1">
                  <h3 className="text-lg font-semibold text-gray-900">
                    {section.title}
                  </h3>
                  {section.subtitle && (
                    <p className="text-sm text-gray-600 mt-0.5">
                      {section.subtitle}
                    </p>
                  )}
                </div>
                
                {section.badge && (
                  <div className="flex-shrink-0">
                    {section.badge}
                  </div>
                )}
              </div>
              
              <div className="flex-shrink-0 ml-3">
                {isExpanded ? (
                  <ChevronUp className="w-5 h-5 text-gray-500" />
                ) : (
                  <ChevronDown className="w-5 h-5 text-gray-500" />
                )}
              </div>
            </button>

            {/* Accordion Content */}
            {isExpanded && (
              <div
                id={`accordion-content-${section.id}`}
                className="px-4 py-4 bg-white border-t border-gray-200"
                role="region"
                aria-labelledby={`accordion-header-${section.id}`}
              >
                {section.content}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default CollapsibleAccordion;
