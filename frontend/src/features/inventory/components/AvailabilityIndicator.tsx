import React from 'react';
import './AvailabilityIndicator.css';
import { AvailabilityIndicatorProps } from '@/types';



const AvailabilityIndicator: React.FC<AvailabilityIndicatorProps> = ({ status }) => {
  const getStatusClass = () => {
    switch (status) {
      case 'DISPONIBLE':
        return 'disponible';
      case 'BAJO_STOCK':
        return 'bajo-stock';
      case 'SIN_STOCK':
        return 'sin-stock';
      default:
        return 'no-info';
    }
  };

  const getStatusText = () => {
    switch (status) {
      case 'DISPONIBLE':
        return 'Disponible';
      case 'BAJO_STOCK':
        return 'Bajo Stock';
      case 'SIN_STOCK':
        return 'Agotado';
      default:
        return 'Sin Info';
    }
  }

  return (
    <span className={`availability-indicator availability-indicator--${getStatusClass()}`}>
      {getStatusText()}
    </span>
  );
};

export default AvailabilityIndicator;
