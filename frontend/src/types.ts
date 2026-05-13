/**
 * Tipos e interfaces para las respuestas y requests de la API.
 * Las propiedades coinciden con el contrato en inglés del backend.
 */

/** Listado consolidado de productos (API: propiedades en inglés). */
export interface AggregatedProductDto {
  id: number;
  mpn: string;
  model: string;
  brand: string;
  category: string | null;
  minCostPrice: number | null;
  minRetailPrice?: number | null;
  availability: string;
  supplierCount: number;
  specifications: { [key: string]: string };
}

export interface AuthResponse {
  token: string;
  tokenType?: string;
  expiresIn?: number;
  username: string;
  roles?: string[];
}

/** Oferta de un proveedor para un producto (API: propiedades en inglés). */
export interface SupplierOfferDto {
  supplierId: number;
  supplierName: string;
  costPrice: number | null;
  retailPrice?: number | null;
  stock: number;
  lastSyncAt: string;
  stockStatus: string;
  ean?: string | null;
  moq?: number | null;
  condition?: string | null;
}

/** Detalle de producto con ofertas por proveedor (API: propiedades en inglés). */
export interface ProductDetailDto {
  id: number;
  mpn: string;
  model: string;
  brand: string;
  category: string | null;
  specifications: { [key: string]: string };
  offers: SupplierOfferDto[];
}

/** Mapeo de campo externo a interno (API: propiedades en inglés). */
export interface SupplierMappingDto {
  mappingId: number;
  supplierId: number;
  internalField: string;
  externalField: string;
  transformationType: 'DIRECT' | 'NESTED' | 'FIND_IN_ARRAY' | 'SPLIT';
}

export interface SupplierMappingRequestDto {
  internalField: string;
  externalField: string;
  transformationType: 'DIRECT' | 'NESTED' | 'FIND_IN_ARRAY' | 'SPLIT';
}

/** Request para crear/actualizar proveedor (API: propiedades en inglés). */
export interface SupplierRequestDto {
  name: string;
  baseUrlApi: string;
  contact?: string;
  email?: string;
  schedule?: string;
  phone?: string;
  website?: string;
  country?: string;
  defaultCurrency?: string;
  apiKey?: string;
  active: boolean;
  supportsBulkSync: boolean;
  catalogEndpoint: string;
  detailEndpoint: string;
  searchEndpoint: string;
}

export interface SupplierResponseDto {
  id: number;
  name: string;
  baseUrlApi: string;
  contact?: string;
  email?: string;
  schedule?: string;
  phone?: string;
  website?: string;
  country?: string;
  defaultCurrency?: string;
  active: boolean;
  supportsBulkSync: boolean;
  catalogEndpoint: string;
  detailEndpoint: string;
  searchEndpoint: string;
}

export interface UserDto {
  id: number;
  username: string;
  email: string;
  fullName?: string;
  roles: string[];
}

export interface UserRequestDto {
  id?: number;
  username: string;
  email: string;
  fullName?: string;
  password?: string;
  roles: string[];
}

export interface SupplierSimpleDto {
  id: number;
  name: string;
}

/** Resumen del dashboard (API: propiedades en inglés). */
export interface DashboardSummaryDto {
  totalProducts: number;
  totalSuppliers: number;
  productsAvailable: number;
  productsLowStock: number;
  productsOutOfStock: number;
  avgLatencyOverall: number;
  productsByBrand: ChartDataDto[];
  stockBySupplier: ChartDataDto[];
  syncHistory: SyncHistoryDto[];
  topExpensiveProducts: TopProductDto[];
  topPriceIncreases: TopMoverDto[];
  topPriceDecreases: TopMoverDto[];
  staleProducts: StaleProductDto[];
  failedProviders: ProviderStatusDto[];
}

export interface ProviderStatusDto {
  id: number;
  name: string;
  lastError: string;
}

export interface ChartDataDto {
  label: string;
  value: number;
}

export interface SyncHistoryDto {
  date: string;
  successCount: number;
  errorCount: number;
}

export interface TopProductDto {
  productId: number;
  name: string;
  supplierName: string;
  price: number;
}

export interface TopMoverDto {
  productId: number;
  productName: string;
  supplierName: string;
  previousPrice: number;
  currentPrice: number;
  percentChange: number;
}

export interface StaleProductDto {
  id: number;
  mpn: string;
  name: string;
  lastUpdatedAt: string;
  daysWithoutUpdate: number;
}

export interface SupplierDashboardDto {
  supplierId: number;
  supplierName: string;
  totalProducts: number;
  productsInStock: number;
  productsOutOfStock: number;
  avgSyncLatencyHours: number;
}

export interface SupplierProduct {
  id: number;
  mpn: string;
  model: string;
  brand: string;
  minCostPrice: number;
  availability: 'DISPONIBLE' | 'BAJO_STOCK' | 'SIN_STOCK' | 'NO_INFO';
  supplierCount: number;
}

/** Estadísticas de precios (API: propiedades en inglés). */
export interface PriceStatisticsDto {
  averagePrice: number;
  minPrice: number;
  maxPrice: number;
  percentVariation: number;
  currentPrice: number;
  initialPrice: number;
  mostRecentDate: string;
  oldestDate: string;
  productName: string;
  supplierName: string;
  totalRecords: number;
}

/** Registro de historial de precio (lista por producto/proveedor). */
export interface PriceHistoryItemDto {
  price: number;
  registeredAt: string;
  trend: string;
}

/** Respuesta completa de un registro de historial de precios (BI). */
export interface PriceHistoryResponseDto {
  historyId: number;
  masterProductId: number;
  productName: string;
  supplierId: number;
  supplierName: string;
  costPrice: number;
  registeredAt: string;
}

export interface DecodedToken {
  sub: string;
  iat: number;
  exp: number;
  roles: string[];
}

export interface AuthContextType {
  token: string | null;
  username: string | null;
  roles: string[];
  login: (authResponse: AuthResponse) => void;
  logout: () => void;
  isAuthenticated: boolean;
  isAdmin: boolean;
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface AvailabilityIndicatorProps {
  status: string;
}

export interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export interface SearchResult {
  content: AggregatedProductDto[];
  totalPages: number;
  totalElements: number;
}

// ============================================================
// Analytics KPIs — Supply Chain
// ============================================================

/**
 * Variacion mensual del indice de precios por proveedor y SKU.
 * Generado con LAG() sobre historial_precios.
 */
export interface PriceIndexVariationDto {
  supplierId: number;
  supplierName: string;
  productId: number;
  mpn: string;
  month: string;
  avgPrice: number;
  prevAvgPrice: number | null;
  variationPct: number | null;
  isOutlier: boolean;
}

/**
 * Tasa de ruptura de stock (Stockout Rate) por proveedor.
 * stockoutRate es un porcentaje de 0.0 a 100.0.
 */
export interface StockoutRateDto {
  supplierId: number;
  supplierName: string;
  totalSkus: number;
  outOfStockSkus: number;
  stockoutRate: number;
}

 

// ============================================================
// KPIs Alta y Media Viabilidad
// ============================================================

/** KPIs operativos de salud de proveedor para el Dashboard. */
export interface SupplierHealthDto {
  supplierId: number;
  supplierName: string;
  slaScore: number;
  syncSuccessRate: number;
  avgItemsPerSync: number;
  avgLatencyMs: number;
  apiErrorRate: number;
  staleRate: number;
  slaGrade: 'A' | 'B' | 'C' | 'D';
}

/** Dispersion de precio entre proveedores para un mismo SKU. */
export interface PriceDispersionDto {
  productId: number;
  mpn: string;
  category: string | null;
  supplierCount: number;
  minPrice: number;
  maxPrice: number;
  avgPrice: number;
  dispersionPct: number;
}

/** Distribucion de MOQ (Minimum Order Quantity) por proveedor. */
export interface MoqDistributionDto {
  supplierId: number;
  supplierName: string;
  avgMoq: number;
  maxMoq: number;
  minMoq: number;
  skusWithMoqAbove10: number;
}

/** Mezcla de condiciones de producto por proveedor. */
export interface ConditionMixDto {
  supplierId: number;
  supplierName: string;
  totalSkus: number;
  newCount: number;
  refurbishedCount: number;
  boxDamagedCount: number;
  usedCount: number;
  newPct: number;
  refurbishedPct: number;
  boxDamagedPct: number;
  usedPct: number;
}

/** Cobertura de proveedor por SKU — riesgo de proveedor unico. */
export interface CostCoverageDto {
  productId: number;
  mpn: string;
  category: string | null;
  totalSuppliers: number;
  suppliersWithStock: number;
  coverageStatus: 'SIN_STOCK' | 'RIESGO_PROVEEDOR_UNICO' | 'CUBIERTO';
}

/** Volatilidad de stock en ventanas de 24 horas. */
export interface StockVolatilityDto {
  supplierId: number;
  supplierName: string;
  productId: number;
  mpn: string;
  changesIn24h: number;
  maxStock: number;
  minStock: number;
  volatilityType: 'INCONSISTENCIA_API' | 'VENTA_PROBABLE' | 'FLUCTUACION_NORMAL';
}

/** Tendencia de crecimiento del catalogo (productos nuevos por semana). */
export interface CatalogGrowthDto {
  year: number;
  week: number;
  newProducts: number;
  weekLabel: string;
}

/** Score de estabilidad de precio basado en coeficiente de variacion (CV). */
export interface PriceStabilityDto {
  productId: number;
  mpn: string;
  supplierId: number;
  supplierName: string;
  avgPrice: number;
  stddevPrice: number;
  cvPct: number;
  stabilityLabel: 'ESTABLE' | 'MODERADO' | 'VOLATIL';
  pricePoints: number;
}
