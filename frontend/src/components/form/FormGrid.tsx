import React from 'react';

interface FormGridProps {
  children: React.ReactNode;
}

/**
 * Contenedor en grid para agrupar campos de formulario en 2–3 columnas
 * manteniendo un diseño consistente entre páginas.
 */
export const FormGrid: React.FC<FormGridProps> = ({ children }) => {
  return <div className="form-grid">{children}</div>;
};

