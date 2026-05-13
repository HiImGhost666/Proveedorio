import './Badge.css';

interface BadgeProps {
  variant?: 'success' | 'error' | 'warning' | 'info' | 'neutral';
  children: React.ReactNode;
  className?: string;
}
export const Badge = ({ variant = 'neutral', children, className = '' }: BadgeProps) => {
  return (
    <span className={`badge badge-${variant} ${className}`}>
      {children}
    </span>
  );
};