export const formatCurrency = (value?: number | null): string => {
  if (value == null) return 'N/A';
  return value.toLocaleString('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });
};
