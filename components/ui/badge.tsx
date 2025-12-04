import React from 'react';

export const Badge = React.forwardRef<HTMLSpanElement, React.HTMLAttributes<HTMLSpanElement>>(
  (props, ref) => <span ref={ref} {...props} />
);
Badge.displayName = 'Badge';
