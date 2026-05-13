/**
 * Rutas y configuración base de la API.
 * Centraliza paths para evitar magic strings en servicios y componentes.
 */

/** Prefijo base de la API para entorno local (sin proxy). */
export const API_BASE = 'http://localhost:8080/api/v1';

/** Segmentos de rutas por recurso. */
export const API_ROUTES = {
  auth: `${API_BASE}/auth`,
  login: `${API_BASE}/auth/login`,
  register: `${API_BASE}/auth/register`,
  validate: `${API_BASE}/auth/validate`,
  refresh: `${API_BASE}/auth/refresh`,

  products: `${API_BASE}/productos`,
  productSearch: `${API_BASE}/productos/search`,

  suppliers: `${API_BASE}/proveedores`,
  supplierList: `${API_BASE}/proveedores/list`,
  supplierMappings: (id: number) => `${API_BASE}/proveedores/${id}/mappings`,

  dashboard: `${API_BASE}/dashboard`,
  dashboardSummary: `${API_BASE}/dashboard/summary`,
  dashboardExport: `${API_BASE}/dashboard/export/csv`,

  priceHistory: `${API_BASE}/historial-precios`,
  priceHistoryByProduct: (id: number) => `${API_BASE}/historial-precios/producto/${id}`,
  priceHistoryByProductAndSupplier: (productId: number, supplierId: number) =>
    `${API_BASE}/historial-precios/producto/${productId}/proveedor/${supplierId}`,
  priceHistoryAnalytics: (productId: number, supplierId: number) =>
    `${API_BASE}/historial-precios/producto/${productId}/proveedor/${supplierId}/analytics`,
  priceHistoryRegister: `${API_BASE}/historial-precios/registrar`,

  adminUsers: `${API_BASE}/admin/usuarios`,
  adminUserById: (id: number) => `${API_BASE}/admin/usuarios/${id}`,
  userProfile: `${API_BASE}/usuarios/perfil`,

  mappings: `${API_BASE}/mappings`,
  mappingById: (id: number) => `${API_BASE}/mappings/${id}`,

  analytics: `/analytics`,
  analyticsPriceVariation: `/analytics/price-variation`,
  analyticsStockoutRates: `/analytics/stockout-rates`,
  analyticsPriceDispersion: `/analytics/price-dispersion`,
  analyticsMoq: `/analytics/moq`,
  analyticsConditionMix: `/analytics/condition-mix`,
  analyticsCostCoverage: `/analytics/cost-coverage`,
  analyticsStockVolatility: `/analytics/stock-volatility`,
  analyticsCatalogGrowth: `/analytics/catalog-growth`,
  analyticsPriceStability: `/analytics/price-stability`,
  dashboardSupplierHealth: `/dashboard/supplier-health`,
} as const;
