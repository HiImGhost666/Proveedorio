import api from 'services/api';
import { API_ROUTES } from 'src/constants/api';
import {
  PriceIndexVariationDto, StockoutRateDto,
  PriceDispersionDto, MoqDistributionDto, ConditionMixDto,
  CostCoverageDto, StockVolatilityDto, CatalogGrowthDto, PriceStabilityDto,
} from 'src/types';

/**
 * Obtiene la variacion mensual del indice de precios por proveedor y SKU.
 * Usa window function LAG() sobre historial_precios.
 *
 * @param months           ventana de analisis en meses (default 6)
 * @param outlierThreshold umbral de variacion para marcar outliers (default 50.0)
 */
export const getPriceIndexVariation = async (
  months = 6,
  outlierThreshold = 50.0
): Promise<PriceIndexVariationDto[]> => {
  const response = await api.get(API_ROUTES.analyticsPriceVariation, {
    params: { months, outlierThreshold },
  });
  return response.data.data;
};

/**
 * Obtiene la tasa de ruptura de stock (Stockout Rate) por proveedor.
 * Ordenado por stockoutRate descendente.
 */
export const getStockoutRates = async (): Promise<StockoutRateDto[]> => {
  const response = await api.get(API_ROUTES.analyticsStockoutRates);
  return response.data.data;
};

// ML dataset API removed — function deprecated and removed.

export const getPriceDispersion = async (maxRows = 100): Promise<PriceDispersionDto[]> => {
  const response = await api.get(API_ROUTES.analyticsPriceDispersion, { params: { maxRows } });
  return response.data.data;
};

export const getMoqDistribution = async (): Promise<MoqDistributionDto[]> => {
  const response = await api.get(API_ROUTES.analyticsMoq);
  return response.data.data;
};

export const getConditionMix = async (): Promise<ConditionMixDto[]> => {
  const response = await api.get(API_ROUTES.analyticsConditionMix);
  return response.data.data;
};

export const getCostCoverage = async (maxRows = 200): Promise<CostCoverageDto[]> => {
  const response = await api.get(API_ROUTES.analyticsCostCoverage, { params: { maxRows } });
  return response.data.data;
};

export const getStockVolatility = async (days = 30, maxRows = 100): Promise<StockVolatilityDto[]> => {
  const response = await api.get(API_ROUTES.analyticsStockVolatility, { params: { days, maxRows } });
  return response.data.data;
};

export const getCatalogGrowth = async (weeks = 12): Promise<CatalogGrowthDto[]> => {
  const response = await api.get(API_ROUTES.analyticsCatalogGrowth, { params: { weeks } });
  return response.data.data;
};

export const getPriceStability = async (days = 90, maxRows = 100): Promise<PriceStabilityDto[]> => {
  const response = await api.get(API_ROUTES.analyticsPriceStability, { params: { days, maxRows } });
  return response.data.data;
};
