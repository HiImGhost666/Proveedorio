import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ArrowUp, ArrowDown, TrendingUp, AlertTriangle, BarChart2, Package, ShieldAlert, Activity, LineChart, Layers, DollarSign, BoxesIcon, BookOpen } from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  Legend, ResponsiveContainer, Cell, AreaChart, Area,
} from 'recharts';

import {
  TopMoverDto, StaleProductDto,
  PriceIndexVariationDto,
  PriceDispersionDto, ConditionMixDto,
  CostCoverageDto, StockVolatilityDto, PriceStabilityDto,
} from 'src/types';
import { getDashboardSummary } from '../services/dashboardService';
import {
  getPriceIndexVariation, getStockoutRates,
  getPriceDispersion, getMoqDistribution, getConditionMix,
  getCostCoverage, getStockVolatility, getCatalogGrowth, getPriceStability,
} from '../services/analyticsService';
import TradingView from '../components/TradingView';
import { formatCurrency } from '@/utils/formatting';
import { DataTable } from '@/components/ui/DataTable';
import './DashboardPage.css';

// ============================================================
// Tipos de pestañas
// ============================================================

type TabId = 'oportunidades' | 'precios' | 'stock' | 'catalogo';

const TABS: { id: TabId; label: string; icon: React.ReactNode }[] = [
  { id: 'oportunidades', label: 'Oportunidades', icon: <Layers     size={16} /> },
  { id: 'precios',       label: 'Precios',        icon: <DollarSign size={16} /> },
  { id: 'stock',         label: 'Stock',           icon: <BoxesIcon  size={16} /> },
  { id: 'catalogo',      label: 'Catálogo',        icon: <BookOpen   size={16} /> },
  
];

// ============================================================
// Shared Layout
// ============================================================

const SectionHeader = ({ icon, title, subtitle }: { icon: React.ReactNode; title: string; subtitle?: string }) => (
  <div className="section-header" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
    <span style={{ color: 'var(--color-primary, #6366f1)' }}>{icon}</span>
    <div>
      <h2 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700 }}>{title}</h2>
      {subtitle && <p style={{ margin: 0, fontSize: '0.8rem', color: '#6b7280' }}>{subtitle}</p>}
    </div>
  </div>
);

const TableCard = ({ title, children, subTitle }: { title: string; children: React.ReactNode; subTitle?: string }) => (
  <div className={`dashboard-card ${subTitle ? 'zombies-card' : ''}`}>
    <h3 className="card-title">{title}</h3>
    {subTitle && <p className="text-sm text-gray-500" style={{ marginTop: '-1rem', marginBottom: '1rem' }}>{subTitle}</p>}
    {children}
  </div>
);

// ============================================================
// Secciones heredadas de OpportunitiesPage
// ============================================================


const TopMoversTable = ({ data, type }: { data: TopMoverDto[]; type: 'up' | 'down' }) => {
  const isUp = type === 'up';
  const Icon = isUp ? ArrowUp : ArrowDown;
  return (
    <DataTable
      columns={[
        { header: 'Producto', cell: (item) => <div className="table-product-name">{item.productName}</div> },
        { header: 'Proveedor', accessorKey: 'supplierName' },
        { header: 'Precio Actual', cell: (item) => formatCurrency(item.currentPrice), align: 'right' },
        {
          header: 'Variación',
          align: 'center',
          cell: (item) => (
            <span className={`price-variation ${isUp ? 'price-up' : 'price-down'}`}>
              <Icon size={14} /> {item.percentChange.toFixed(2)}%
            </span>
          ),
        },
        {
          header: 'Acciones',
          align: 'center',
          cell: (item) =>
            item.productId ? (
              <Link to={`/product/${item.productId}`}><button className="btn-action btn-action-edit">Ver Detalle</button></Link>
            ) : null,
        },
      ]}
      data={data}
      emptyMessage="No hay datos de movimiento de precios."
      keyExtractor={(item) => item.productId ?? `${item.productName}-${item.supplierName}`}
    />
  );
};

const ZombiesTable = ({ products }: { products: StaleProductDto[] }) => {
  if (!products || products.length === 0) return null;
  return (
    <DataTable
      columns={[
        { header: 'MPN', cell: (p) => <span className="font-mono text-xs">{p.mpn}</span> },
        { header: 'Producto', cell: (p) => <span className="table-product-name">{p.name}</span> },
        { header: 'Última Act.', cell: (p) => new Date(p.lastUpdatedAt).toLocaleDateString() },
        {
          header: 'Días Inactivo',
          align: 'center',
          cell: (p) => <span className="inactive-days-badge">{p.daysWithoutUpdate} días</span>,
        },
        {
          header: 'Acciones',
          align: 'center',
          cell: (p) =>
            p.id ? (
              <Link to={`/product/${p.id}`}><button className="btn-action btn-action-edit">Ver Detalle</button></Link>
            ) : null,
        },
      ]}
      data={products}
      emptyMessage="No hay productos zombies."
      keyExtractor={(p) => p.id}
    />
  );
};

// ============================================================
// KPI A — Price Index Variation
// ============================================================

const COLORS = ['#6366f1', '#f59e0b', '#10b981', '#ef4444', '#3b82f6', '#8b5cf6'];

const PriceVariationChart = ({ data }: { data: PriceIndexVariationDto[] }) => {
  const [monthsFilter, setMonthsFilter] = useState(6);
  const [lastValid, setLastValid] = useState<PriceIndexVariationDto[]>(data);
  const { data: fresh, isLoading, error } = useQuery({
    queryKey: ['analytics-price-variation', monthsFilter],
    queryFn: () => getPriceIndexVariation(monthsFilter, 50),
    initialData: data,
  });

  useEffect(() => {
    if (fresh && fresh.length > 0) setLastValid(fresh);
  }, [fresh]);

  const showData = (!error && fresh && fresh.length > 0) ? fresh : lastValid;

  if (isLoading && !showData.length) return <div className="centered-message">Calculando variaciones...</div>;
  if (error && !showData.length) return <div className="centered-message error-message">Error al cargar variaciones de precio.</div>;

  const byMonth: Record<string, Record<string, number[]>> = {};
  const suppliers = new Set<string>();
  for (const row of showData) {
    if (row.variationPct === null) continue;
    if (!byMonth[row.month]) byMonth[row.month] = {};
    if (!byMonth[row.month][row.supplierName]) byMonth[row.month][row.supplierName] = [];
    byMonth[row.month][row.supplierName].push(row.variationPct);
    suppliers.add(row.supplierName);
  }

  const chartData = Object.entries(byMonth)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([month, supplierMap]) => {
      const entry: Record<string, number | string> = { month };
      for (const [supplier, values] of Object.entries(supplierMap)) {
        entry[supplier] = parseFloat((values.reduce((a, b) => a + b, 0) / values.length).toFixed(2));
      }
      return entry;
    });

  const supplierList = Array.from(suppliers);
  const outlierCount = showData.filter((r) => r.isOutlier).length;

  return (
    <div className="dashboard-card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
        <h3 className="card-title" style={{ margin: 0 }}>Variación Mensual de Índice de Precios</h3>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {outlierCount > 0 && (
            <span style={{ fontSize: '0.75rem', background: '#fef3c7', color: '#92400e', padding: '2px 8px', borderRadius: '9999px' }}>
              {outlierCount} outliers filtrados
            </span>
          )}
          <select
            value={monthsFilter}
            onChange={(e) => setMonthsFilter(Number(e.target.value))}
            style={{ fontSize: '0.8rem', padding: '4px 8px', borderRadius: '6px', border: '1px solid #e5e7eb' }}
          >
            <option value={3}>3 meses</option>
            <option value={6}>6 meses</option>
            <option value={12}>12 meses</option>
          </select>
        </div>
      </div>
      {chartData.length === 0 ? (
        <div className="centered-message">Sin datos de historial de precios en el periodo seleccionado.</div>
      ) : (
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="month" tick={{ fontSize: 12 }} />
            <YAxis tickFormatter={(v) => `${v}%`} tick={{ fontSize: 12 }} />
            <Tooltip formatter={(v: number | undefined) => [`${(v ?? 0).toFixed(2)}%`, 'Variación avg']} />
            <Legend />
            {supplierList.map((name, i) => (
              <Bar key={name} dataKey={name} fill={COLORS[i % COLORS.length]} radius={[3, 3, 0, 0]}>
                {chartData.map((entry, idx) => {
                  const val = entry[name] as number;
                  const isHigh = val > 20;
                  return <Cell key={idx} fill={isHigh ? '#ef4444' : COLORS[i % COLORS.length]} />;
                })}
              </Bar>
            ))}
          </BarChart>
        </ResponsiveContainer>
      )}
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        Barras en rojo: variacion &gt;20%. Outliers (&gt;50%) excluidos automaticamente.
        Precios normalizados a EUR via tabla currency_rates.
      </p>
    </div>
  );
};

// ============================================================
// KPI B — Stockout Rate
// ============================================================

const stockoutColor = (rate: number) => {
  if (rate >= 50) return { bg: '#fef2f2', border: '#fca5a5', text: '#b91c1c', label: 'Critico' };
  if (rate >= 25) return { bg: '#fff7ed', border: '#fdba74', text: '#c2410c', label: 'Alto' };
  if (rate >= 10) return { bg: '#fefce8', border: '#fde047', text: '#92400e', label: 'Moderado' };
  return { bg: '#f0fdf4', border: '#86efac', text: '#166534', label: 'Bajo' };
};

const StockoutRateSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-stockout-rates'],
    queryFn: getStockoutRates,
  });

  if (isLoading) return <div className="centered-message">Calculando tasas de stockout...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Stockout Rates.</div>;

  return (
    <div className="dashboard-card">
      <h3 className="card-title">Stockout Rate por Proveedor</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '1rem' }}>
        {data.map((dto) => {
          const colors = stockoutColor(dto.stockoutRate);
          return (
            <div
              key={dto.supplierId}
              style={{
                background: colors.bg,
                border: `1px solid ${colors.border}`,
                borderRadius: '10px',
                padding: '1rem',
              }}
            >
              <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#1f2937', marginBottom: '0.25rem' }}>
                {dto.supplierName}
              </div>
              <div style={{ fontSize: '1.8rem', fontWeight: 800, color: colors.text, lineHeight: 1 }}>
                {dto.stockoutRate.toFixed(1)}%
              </div>
              <div style={{ fontSize: '0.72rem', color: '#6b7280', marginTop: '0.25rem' }}>
                {dto.outOfStockSkus} / {dto.totalSkus} SKUs sin stock
              </div>
              <div style={{
                marginTop: '0.5rem',
                display: 'inline-block',
                fontSize: '0.7rem',
                fontWeight: 700,
                color: colors.text,
                background: 'white',
                border: `1px solid ${colors.border}`,
                borderRadius: '9999px',
                padding: '1px 8px',
              }}>
                {colors.label}
              </div>
            </div>
          );
        })}
      </div>
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '1rem' }}>
        Verde &lt;10% · Amarillo 10-25% · Naranja 25-50% · Rojo ≥50%
      </p>
    </div>
  );
};

// ML Dataset feature removed from UI.

// ============================================================
// KPI — Price Dispersion Index
// ============================================================

const PriceDispersionSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-price-dispersion'],
    queryFn: () => getPriceDispersion(100),
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando dispersión de precios...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Price Dispersion.</div>;
  return (
    <div className="dashboard-card" style={{ overflowX: 'auto' }}>
      <h3 className="card-title">Price Dispersion Index — SKUs con mayor diferencia de precio entre proveedores</h3>
      <DataTable
        columns={[
          { header: 'MPN', cell: (r: PriceDispersionDto) => <span className="font-mono text-xs">{r.mpn}</span> },
          { header: 'Categoría', cell: (r: PriceDispersionDto) => r.category ?? '—' },
          { header: 'Proveedores', cell: (r: PriceDispersionDto) => r.supplierCount, align: 'center' },
          { header: 'Min €', cell: (r: PriceDispersionDto) => formatCurrency(r.minPrice), align: 'right' },
          { header: 'Max €', cell: (r: PriceDispersionDto) => formatCurrency(r.maxPrice), align: 'right' },
          { header: 'Avg €', cell: (r: PriceDispersionDto) => formatCurrency(r.avgPrice), align: 'right' },
          {
            header: 'Dispersión',
            align: 'right',
            cell: (r: PriceDispersionDto) => (
              <span style={{ fontWeight: 700, color: r.dispersionPct > 30 ? '#dc2626' : r.dispersionPct > 15 ? '#f59e0b' : '#10b981' }}>
                {r.dispersionPct.toFixed(1)}%
              </span>
            ),
          },
        ]}
        data={data}
        emptyMessage="No hay SKUs con 2+ proveedores todavía."
        keyExtractor={(r: PriceDispersionDto) => r.productId}
      />
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        Solo SKUs con 2+ proveedores. Alto % = oportunidad de negociación.
      </p>
    </div>
  );
};

// ============================================================
// KPI — MOQ Distribution
// ============================================================

const MoqDistributionSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-moq'],
    queryFn: getMoqDistribution,
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando MOQ...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar MOQ Distribution.</div>;
  return (
    <div className="dashboard-card">
      <h3 className="card-title">MOQ Distribution — Cantidad mínima de pedido por proveedor</h3>
      {data.length === 0 ? (
        <div className="centered-message">Sin datos de MOQ.</div>
      ) : (
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="supplierName" tick={{ fontSize: 11 }} />
            <YAxis tick={{ fontSize: 12 }} />
            <Tooltip formatter={(v: number | undefined) => [(v ?? 0).toFixed(1), 'Avg MOQ']} />
            <Legend />
            <Bar dataKey="avgMoq" fill="#6366f1" name="MOQ Promedio" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      )}
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        MOQ alto = barrera de compra. Considera negociar pedidos mínimos con proveedores de MOQ &gt; 10.
      </p>
    </div>
  );
};

// ============================================================
// KPI — Condition Mix
// ============================================================

const CONDITION_COLORS = { NEW: '#10b981', REFURBISHED: '#3b82f6', BOX_DAMAGED: '#f59e0b', USED: '#ef4444' };

const ConditionMixSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-condition-mix'],
    queryFn: getConditionMix,
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando Condition Mix...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Condition Mix.</div>;
  const chartData = data.map((r: ConditionMixDto) => ({
    name: r.supplierName,
    NEW: r.newPct,
    REFURBISHED: r.refurbishedPct,
    BOX_DAMAGED: r.boxDamagedPct,
    USED: r.usedPct,
  }));
  return (
    <div className="dashboard-card">
      <h3 className="card-title">Condition Mix — Distribución de condición de productos por proveedor</h3>
      {chartData.length === 0 ? (
        <div className="centered-message">Sin datos de condición.</div>
      ) : (
        <ResponsiveContainer width="100%" height={240}>
          <BarChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" tick={{ fontSize: 11 }} />
            <YAxis tickFormatter={(v) => `${v}%`} tick={{ fontSize: 12 }} domain={[0, 100]} />
            <Tooltip formatter={(v: number | undefined) => [`${(v ?? 0).toFixed(1)}%`]} />
            <Legend />
            {Object.entries(CONDITION_COLORS).map(([key, color]) => (
              <Bar key={key} dataKey={key} stackId="a" fill={color} name={key} radius={key === 'USED' ? [4, 4, 0, 0] : [0, 0, 0, 0]} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  );
};

// ============================================================
// KPI — Cost Coverage Ratio
// ============================================================

const coverageBadge = (status: string) => {
  if (status === 'SIN_STOCK') return { bg: '#fef2f2', color: '#b91c1c', label: 'Sin Stock' };
  if (status === 'RIESGO_PROVEEDOR_UNICO') return { bg: '#fff7ed', color: '#c2410c', label: 'Proveedor Único' };
  return { bg: '#f0fdf4', color: '#166534', label: 'Cubierto' };
};

const CostCoverageSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-cost-coverage'],
    queryFn: () => getCostCoverage(200),
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando cobertura de proveedores...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Cost Coverage.</div>;
  const risky = data.filter((r: CostCoverageDto) => r.coverageStatus !== 'CUBIERTO');
  return (
    <div className="dashboard-card" style={{ overflowX: 'auto' }}>
      <h3 className="card-title">
        Cost Coverage Ratio — SKUs en riesgo
        {risky.length > 0 && (
          <span style={{ marginLeft: '0.75rem', fontSize: '0.75rem', background: '#fee2e2', color: '#b91c1c', padding: '2px 8px', borderRadius: '9999px' }}>
            {risky.length} en riesgo
          </span>
        )}
      </h3>
      <DataTable
        columns={[
          { header: 'MPN', cell: (r: CostCoverageDto) => <span className="font-mono text-xs">{r.mpn}</span> },
          { header: 'Categoría', cell: (r: CostCoverageDto) => r.category ?? '—' },
          { header: 'Proveedores', cell: (r: CostCoverageDto) => r.totalSuppliers, align: 'center' },
          { header: 'Con Stock', cell: (r: CostCoverageDto) => r.suppliersWithStock, align: 'center' },
          {
            header: 'Estado',
            align: 'center',
            cell: (r: CostCoverageDto) => {
              const b = coverageBadge(r.coverageStatus);
              return (
                <span style={{ background: b.bg, color: b.color, fontWeight: 700, fontSize: '0.7rem', padding: '2px 8px', borderRadius: '9999px' }}>
                  {b.label}
                </span>
              );
            },
          },
        ]}
        data={risky.length > 0 ? risky : data.slice(0, 50)}
        emptyMessage="Todos los SKUs tienen cobertura adecuada."
        keyExtractor={(r: CostCoverageDto) => r.productId}
      />
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        Mostrando SKUs en riesgo (sin stock o proveedor único). {data.length} SKUs totales analizados.
      </p>
    </div>
  );
};

// ============================================================
// KPI — Stock Volatility Index
// ============================================================

const volatilityBadge = (type: string) => {
  if (type === 'INCONSISTENCIA_API') return { bg: '#fef2f2', color: '#b91c1c', label: 'Inconsistencia API' };
  if (type === 'VENTA_PROBABLE') return { bg: '#eff6ff', color: '#1d4ed8', label: 'Venta Probable' };
  return { bg: '#f9fafb', color: '#6b7280', label: 'Fluctuación Normal' };
};

const StockVolatilitySection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-stock-volatility'],
    queryFn: () => getStockVolatility(30, 100),
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Analizando volatilidad de stock...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Stock Volatility.</div>;
  return (
    <div className="dashboard-card" style={{ overflowX: 'auto' }}>
      <h3 className="card-title">Stock Volatility Index — SKUs con alta volatilidad (últimos 30 días)</h3>
      {data.length === 0 ? (
        <p style={{ color: '#6b7280', fontSize: '0.875rem' }}>
          Sin volatilidad detectada. Los datos se acumulan en cada sincronización automática.
        </p>
      ) : (
        <DataTable
          columns={[
            { header: 'MPN', cell: (r: StockVolatilityDto) => <span className="font-mono text-xs">{r.mpn}</span> },
            { header: 'Proveedor', cell: (r: StockVolatilityDto) => r.supplierName },
            { header: 'Cambios/24h', cell: (r: StockVolatilityDto) => r.changesIn24h, align: 'center' },
            { header: 'Stock Max', cell: (r: StockVolatilityDto) => r.maxStock, align: 'center' },
            { header: 'Stock Min', cell: (r: StockVolatilityDto) => r.minStock, align: 'center' },
            {
              header: 'Tipo',
              align: 'center',
              cell: (r: StockVolatilityDto) => {
                const b = volatilityBadge(r.volatilityType);
                return (
                  <span style={{ background: b.bg, color: b.color, fontWeight: 700, fontSize: '0.7rem', padding: '2px 8px', borderRadius: '9999px' }}>
                    {b.label}
                  </span>
                );
              },
            },
          ]}
          data={data}
          emptyMessage="Sin volatilidad detectada."
          keyExtractor={(r: StockVolatilityDto) => `${r.productId}-${r.supplierId}`}
        />
      )}
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        Inconsistencia API: stock 100→0→≥50 en &lt;24h. Venta Probable: bajada sin recuperación.
      </p>
    </div>
  );
};

// ============================================================
// KPI — Catalog Growth Trend
// ============================================================

const CatalogGrowthSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-catalog-growth'],
    queryFn: () => getCatalogGrowth(12),
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando crecimiento del catálogo...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Catalog Growth.</div>;
  return (
    <div className="dashboard-card">
      <h3 className="card-title">Catalog Growth Trend — Productos nuevos por semana (últimas 12 semanas)</h3>
      {data.length === 0 ? (
        <p style={{ color: '#6b7280', fontSize: '0.875rem' }}>Sin datos de crecimiento del catálogo aún.</p>
      ) : (
        <ResponsiveContainer width="100%" height={220}>
          <AreaChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="weekLabel" tick={{ fontSize: 11 }} />
            <YAxis tick={{ fontSize: 12 }} />
            <Tooltip formatter={(v: number | undefined) => [v ?? 0, 'Productos nuevos']} />
            <Area type="monotone" dataKey="newProducts" stroke="#6366f1" fill="#e0e7ff" name="Productos nuevos" />
          </AreaChart>
        </ResponsiveContainer>
      )}
    </div>
  );
};

// ============================================================
// KPI — Price Stability Score
// ============================================================

const stabilityBadgeStyle = (label: string) => {
  if (label === 'VOLATIL') return { bg: '#fef2f2', color: '#b91c1c' };
  if (label === 'MODERADO') return { bg: '#fefce8', color: '#92400e' };
  return { bg: '#f0fdf4', color: '#166534' };
};

const PriceStabilitySection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-price-stability'],
    queryFn: () => getPriceStability(90, 100),
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando estabilidad de precios...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Price Stability.</div>;
  return (
    <div className="dashboard-card" style={{ overflowX: 'auto' }}>
      <h3 className="card-title">Price Stability Score — SKUs por coeficiente de variación (90 días)</h3>
      {data.length === 0 ? (
        <p style={{ color: '#6b7280', fontSize: '0.875rem' }}>
          Insuficiente historial de precios. Necesita al menos 2 registros por SKU/proveedor.
        </p>
      ) : (
        <DataTable
          columns={[
            { header: 'MPN', cell: (r: PriceStabilityDto) => <span className="font-mono text-xs">{r.mpn}</span> },
            { header: 'Proveedor', cell: (r: PriceStabilityDto) => r.supplierName },
            { header: 'Precio Avg', cell: (r: PriceStabilityDto) => formatCurrency(r.avgPrice), align: 'right' },
            { header: 'Stddev', cell: (r: PriceStabilityDto) => formatCurrency(r.stddevPrice), align: 'right' },
            { header: 'CV %', cell: (r: PriceStabilityDto) => `${r.cvPct.toFixed(1)}%`, align: 'center' },
            { header: 'Puntos', cell: (r: PriceStabilityDto) => r.pricePoints, align: 'center' },
            {
              header: 'Estabilidad',
              align: 'center',
              cell: (r: PriceStabilityDto) => {
                const s = stabilityBadgeStyle(r.stabilityLabel);
                return (
                  <span style={{ background: s.bg, color: s.color, fontWeight: 700, fontSize: '0.7rem', padding: '2px 8px', borderRadius: '9999px' }}>
                    {r.stabilityLabel}
                  </span>
                );
              },
            },
          ]}
          data={data}
          emptyMessage="Sin historial suficiente."
          keyExtractor={(r: PriceStabilityDto) => `${r.productId}-${r.supplierId}`}
        />
      )}
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        CV: Coeficiente de variación (stddev / avg). ESTABLE &lt;5% · MODERADO 5-15% · VOLÁTIL &gt;15%.
      </p>
    </div>
  );
};

// ============================================================
// Main AnalyticsPage
// ============================================================

const AnalyticsPage = () => {
  const [activeTab, setActiveTab] = useState<TabId>('oportunidades');

  const { data: summary, isLoading, error } = useQuery({
    queryKey: ['dashboardSummary'],
    queryFn: getDashboardSummary,
  });

  const isDataReady = !isLoading && !error && summary;

  const tabStyle = (id: TabId): React.CSSProperties => ({
    display: 'flex',
    alignItems: 'center',
    padding: '0.5rem 1.25rem',
    borderRadius: '8px',
    border: 'none',
    cursor: 'pointer',
    fontWeight: activeTab === id ? 600 : 400,
    backgroundColor: activeTab === id ? 'var(--color-primary, #6366f1)' : 'transparent',
    color: activeTab === id ? 'white' : 'var(--text-secondary, #6b7280)',
    fontSize: '0.875rem',
    transition: 'all 0.15s ease',
    whiteSpace: 'nowrap',
    boxShadow: activeTab === id ? '0 1px 4px rgba(99,102,241,0.35)' : 'none',
  });

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1 className="dashboard-title">Analytics &amp; KPIs</h1>
        <p style={{ margin: 0, color: '#6b7280', fontSize: '0.9rem' }}>
          Análisis de precios, disponibilidad y datos para modelos de predicción de compras
        </p>
      </header>

      {/* Barra de pestañas */}
      <nav style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexWrap: 'wrap',
        gap: '0',
        padding: '0.5rem 1rem',
        backgroundColor: 'var(--bg-secondary)',
        borderRadius: '12px',
        border: '1px solid var(--bg-tertiary)',
        marginBottom: '2rem',
      }}>
        {TABS.map((tab, index) => (
          <div key={tab.id} style={{ display: 'flex', alignItems: 'center' }}>
            <button style={tabStyle(tab.id)} onClick={() => setActiveTab(tab.id)}>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                {tab.icon}
                {tab.label}
              </span>
            </button>
            {index < TABS.length - 1 && (
              <span style={{
                width: '1px',
                height: '1.25rem',
                backgroundColor: 'var(--bg-tertiary)',
                margin: '0 0.125rem',
                flexShrink: 0,
              }} />
            )}
          </div>
        ))}
      </nav>

      <main style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>

        {/* ── Oportunidades ── */}
        {activeTab === 'oportunidades' && (
          <>
            <section>
              <SectionHeader icon={<TrendingUp size={20} />} title="Análisis de Margen y Oportunidades de Compra" />
              <TradingView />
            </section>

            {isDataReady && (
              <section>
                <SectionHeader icon={<AlertTriangle size={20} />} title="Alertas de Precio en Tiempo Real" />
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                  <TableCard title="Alertas: Subidas de Precio">
                    <TopMoversTable data={summary.topPriceIncreases} type="up" />
                  </TableCard>
                  <TableCard title="Oportunidades: Bajadas de Precio">
                    <TopMoversTable data={summary.topPriceDecreases} type="down" />
                  </TableCard>
                  {summary.staleProducts?.length > 0 && (
                    <TableCard
                      title='Productos "Zombies"'
                      subTitle="Productos sin actualización en +30 días. Revisa la conexión con el proveedor."
                    >
                      <ZombiesTable products={summary.staleProducts} />
                    </TableCard>
                  )}
                </div>
              </section>
            )}
            {!isDataReady && isLoading && (
              <div className="centered-message">Cargando datos de oportunidades...</div>
            )}
          </>
        )}

        {/* ── Precios ── */}
        {activeTab === 'precios' && (
          <>
            <section>
              <SectionHeader
                icon={<TrendingUp size={20} />}
                title="Price Index Variation por Proveedor"
                subtitle="Variación mensual de precios promedio (LAG). Precios normalizados a EUR."
              />
              {isDataReady
                ? <PriceVariationChart data={[]} />
                : <div className="centered-message">Cargando...</div>
              }
            </section>
            <section>
              <SectionHeader
                icon={<BarChart2 size={20} />}
                title="Price Dispersion Index"
                subtitle="SKUs con mayor diferencia de precio entre proveedores. Alto % = oportunidad de negociación."
              />
              <PriceDispersionSection />
            </section>
            <section>
              <SectionHeader
                icon={<LineChart size={20} />}
                title="Price Stability Score"
                subtitle="SKUs ordenados por coeficiente de variación del precio en los últimos 90 días."
              />
              <PriceStabilitySection />
            </section>
          </>
        )}

        {/* ── Stock ── */}
        {activeTab === 'stock' && (
          <>
            <section>
              <SectionHeader
                icon={<AlertTriangle size={20} />}
                title="Stockout Rate por Proveedor"
                subtitle="Tasa de SKUs con stock = 0. Un stockout es pérdida de oportunidad de venta."
              />
              <StockoutRateSection />
            </section>
            <section>
              <SectionHeader
                icon={<AlertTriangle size={20} />}
                title="Stock Volatility Index"
                subtitle="SKUs con fluctuaciones de stock anómalas en ventanas de 24 horas. Detecta inconsistencias de API."
              />
              <StockVolatilitySection />
            </section>
            <section>
              <SectionHeader
                icon={<ShieldAlert size={20} />}
                title="Cost Coverage Ratio"
                subtitle="SKUs en riesgo por dependencia de proveedor único o sin stock disponible."
              />
              <CostCoverageSection />
            </section>
          </>
        )}

        {/* ── Catálogo ── */}
        {activeTab === 'catalogo' && (
          <>
            <section>
              <SectionHeader
                icon={<Activity size={20} />}
                title="Condition Mix"
                subtitle="Distribución de condición de productos (NEW/REFURBISHED/BOX_DAMAGED/USED) por proveedor."
              />
              <ConditionMixSection />
            </section>
            <section>
              <SectionHeader
                icon={<Package size={20} />}
                title="MOQ Distribution"
                subtitle="Cantidad mínima de pedido promedio por proveedor. MOQ alto = barrera de entrada."
              />
              <MoqDistributionSection />
            </section>
            <section>
              <SectionHeader
                icon={<TrendingUp size={20} />}
                title="Catalog Growth Trend"
                subtitle="Evolución del número de productos nuevos incorporados al catálogo, semana a semana."
              />
              <CatalogGrowthSection />
            </section>
          </>
        )}

        {/* ML Dataset removed */}

      </main>
    </div>
  );
};

export default AnalyticsPage;