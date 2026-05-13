import { useState } from 'react';

/**
 * Hook genérico para gestionar errores de campos de formulario.
 * Permite asociar mensajes de error a cada campo y exponer props de accesibilidad.
 */
export function useFieldErrors<TField extends string>() {
  type ErrorMap = Partial<Record<TField, string>>;

  const [errors, setErrors] = useState<ErrorMap>({});

  const setFieldError = (field: TField, message: string) => {
    setErrors((prev) => ({ ...prev, [field]: message }));
  };

  const clearFieldError = (field: TField) => {
    setErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  const clearAllErrors = () => setErrors({});

  /**
   * Devuelve props de accesibilidad para asociar el input con su mensaje de error.
   */
  const bindField = (field: TField) => ({
    'aria-invalid': errors[field] ? true : undefined,
    'aria-describedby': errors[field] ? `error-${field}` : undefined,
  });

  return {
    errors,
    setFieldError,
    clearFieldError,
    clearAllErrors,
    bindField,
  };
}

