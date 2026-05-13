import React from 'react';

interface FormActionsProps {
  children: React.ReactNode;
}

/**
 * Contenedor para los botones de acción de un formulario (Guardar, Cancelar, etc.).
 * Asegura alineación y espaciados homogéneos.
 */
export const FormActions: React.FC<FormActionsProps> = ({ children }) => {
  return <div className="form-actions">{children}</div>;
};

