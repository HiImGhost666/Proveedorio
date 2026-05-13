import api from 'services/api';
import { API_ROUTES } from 'src/constants/api';
import { SupplierHealthDto } from 'src/types';

/**
 * Obtiene los KPIs de salud operativa de todos los proveedores.
 * Solo disponible para el rol ADMIN.
 * Incluye: SLA Score, Sync Success Rate, Avg Items/Sync, Avg Latency, API Error Rate, Stale Rate.
 */
export const getSupplierHealth = async (): Promise<SupplierHealthDto[]> => {
  const response = await api.get(API_ROUTES.dashboardSupplierHealth);
  return response.data.data;
};
