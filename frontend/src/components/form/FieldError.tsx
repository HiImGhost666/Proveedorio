import React from 'react';

interface FieldErrorProps {
  field: string;
  message?: string;
}

/**
 * Componente visual para mostrar el mensaje de error de un campo concreto.
 * Se renderiza debajo del input asociado y usa atributos ARIA para accesibilidad.
 */
export const FieldError: React.FC<FieldErrorProps> = ({ field, message }) => {
  if (!message) return null;

  return (
    <p
      id={`error-${field}`}
      className="field-error"
      role="alert"
    >
      {message}
    </p>
  );
};

