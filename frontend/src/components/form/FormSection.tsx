import React from 'react';

interface FormSectionProps {
  title: string;
  children: React.ReactNode;
}

/**
 * Sección de formulario con título consistente.
 * Unifica el estilo de bloques como "Información del usuario" o "Información general".
 */
export const FormSection: React.FC<FormSectionProps> = ({ title, children }) => {
  return (
    <div className="form-section">
      <h2 className="section-title">{title}</h2>
      {children}
    </div>
  );
};

