import React from 'react';

export const Table = React.forwardRef<HTMLTableElement, React.TableHTMLAttributes<HTMLTableElement>>(
  ({ className = '', ...props }, ref) => (
    <table ref={ref} className={`w-full border-collapse ${className}`} {...props} />
  )
);
Table.displayName = 'Table';

export const TableHeader = React.forwardRef<HTMLTableSectionElement, React.HTMLAttributes<HTMLTableSectionElement>>(
  ({ className = '', ...props }, ref) => (
    <thead ref={ref} className={`bg-[#fbfbfb] ${className}`} {...props} />
  )
);
TableHeader.displayName = 'TableHeader';

export const TableBody = React.forwardRef<HTMLTableSectionElement, React.HTMLAttributes<HTMLTableSectionElement>>(
  ({ className = '', ...props }, ref) => (
    <tbody ref={ref} className={`divide-y divide-[#dcdede] ${className}`} {...props} />
  )
);
TableBody.displayName = 'TableBody';

export const TableRow = React.forwardRef<HTMLTableRowElement, React.HTMLAttributes<HTMLTableRowElement>>(
  ({ className = '', ...props }, ref) => (
    <tr ref={ref} className={`hover:bg-[#f8f9fa] transition-colors border-b border-[#dcdede] ${className}`} {...props} />
  )
);
TableRow.displayName = 'TableRow';

export const TableHead = React.forwardRef<HTMLTableCellElement, React.ThHTMLAttributes<HTMLTableCellElement>>(
  ({ className = '', ...props }, ref) => (
    <th ref={ref} className={`px-4 py-3 text-left text-xs font-medium text-[#5d6567] uppercase tracking-wider ${className}`} {...props} />
  )
);
TableHead.displayName = 'TableHead';

export const TableCell = React.forwardRef<HTMLTableCellElement, React.TdHTMLAttributes<HTMLTableCellElement>>(
  ({ className = '', ...props }, ref) => (
    <td ref={ref} className={`px-4 py-3 text-sm text-[#0f1012] ${className}`} {...props} />
  )
);
TableCell.displayName = 'TableCell';
