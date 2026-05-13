import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

import api from 'services/api';
import './PriceHistoryPage.css';
import { formatCurrency } from '@/utils/formatting';
import { PriceStatisticsDto, PriceHistoryResponseDto } from '@/types';
import { DataTable } from '@/components/ui/DataTable';




const PriceHistoryPage = () => {
  const { productoId, proveedorId } = useParams<{ productoId: string; proveedorId: string }>();

  // Fetch del histórico
  const { data: historial, isLoading: historialLoading } = useQuery({
    queryKey: ['priceHistory', productoId, proveedorId],
    queryFn: async () => {
      const response = await api.get(
        `/historial-precios/producto/${productoId}/proveedor/${proveedorId}`
      );
      return response.data.data as PriceHistoryResponseDto[];
    },
    enabled: !!productoId && !!proveedorId,
  });

  // Fetch de estadísticas (Analytics)
  const { data: estadisticas, isLoading: estadisticasLoading } = useQuery({
    queryKey: ['priceAnalytics', productoId, proveedorId],
    queryFn: async () => {
      const response = await api.get(
        `/historial-precios/producto/${productoId}/proveedor/${proveedorId}/analytics`
      );
      return response.data.data as PriceStatisticsDto;
    },
    enabled: !!productoId && !!proveedorId,
  });

  const isLoading = historialLoading || estadisticasLoading;

  if (isLoading) {
    return <div className="loading-message">Cargando histórico de precios...</div>;
  }

  if (!estadisticas) {
    return <div className="error-message">Error al cargar el histórico de precios.</div>;
  }

  // Determinar color de variación
  const variacionReal =
    estadisticas.initialPrice !== 0
      ? ((estadisticas.currentPrice - estadisticas.initialPrice) / estadisticas.initialPrice) * 100
      : 0;
  const variacionColor = variacionReal > 0 ? '#ef4444' : variacionReal < 0 ? '#10b981' : '#666';
  const variacionSymbol = variacionReal > 0 ? '↑' : variacionReal < 0 ? '↓' : '=';

  return (
    <div className="price-history-container">
      <div className="price-history-card">
        {/* Header */}
        <div className="price-history-header">
          <div className="mb-6">
            <Link to={`/product/${productoId}`} className="back-link">
              &larr; Volver al producto
            </Link>
          </div>

          <h1 className="price-history-title">Histórico de Precios</h1>
          <p className="price-history-subtitle">
            {estadisticas.productName} - {estadisticas.supplierName}
          </p>
        </div>

        {/* Tarjetas de Estadísticas */}
        <div className="stats-grid">
          <div className="stat-card">
            <p className="stat-label">Precio Actual</p>
            <p className="stat-value">
              {formatCurrency(estadisticas.currentPrice)}
            </p>
          </div>

          <div className="stat-card">
            <p className="stat-label">Precio Promedio</p>
            <p className="stat-value">
              {formatCurrency(estadisticas.averagePrice)}
            </p>
          </div>

          <div className="stat-card">
            <p className="stat-label">Precio Mínimo</p>
            <p className="stat-value stat-value-good">
              {formatCurrency(estadisticas.minPrice)}
            </p>
          </div>

          <div className="stat-card">
            <p className="stat-label">Precio Máximo</p>
            <p className="stat-value stat-value-warning">
              {formatCurrency(estadisticas.maxPrice)}
            </p>
          </div>

          <div className="stat-card">
            <p className="stat-label">Variación Total</p>
            <p className="stat-value" style={{ color: variacionColor }}>
              {variacionSymbol} {Math.abs(variacionReal).toFixed(2)}%
            </p>
          </div>

          <div className="stat-card">
            <p className="stat-label">Registros</p>
            <p className="stat-value">{estadisticas.totalRecords}</p>
          </div>
        </div>

        {/* Tabla de Histórico (DataTable) */}
        <section className="history-section">
          <h2 className="history-title">Histórico Detallado</h2>
          <DataTable<PriceHistoryResponseDto>
            columns={[
              {
                header: 'Fecha',
                cell: (registro) => (
                  <span className="fecha-cell">
                    {new Date(registro.registeredAt).toLocaleDateString('es-ES')}
                  </span>
                ),
              },
              {
                header: 'Precio',
                align: 'right',
                cell: (registro) => (
                  <span className="precio-cell">
                    {formatCurrency(registro.costPrice)}
                  </span>
                ),
              },
              {
                header: 'Cambio',
                align: 'right',
                cell: (registro, index) => {
                  const precioAnterior =
                    historial && historial[index + 1]?.costPrice !== undefined
                      ? historial[index + 1].costPrice
                      : registro.costPrice;
                  const cambio = Number(registro.costPrice) - Number(precioAnterior);
                  const cambioColor = cambio > 0 ? '#ef4444' : cambio < 0 ? '#10b981' : '#999';
                  const cambioSymbol = cambio > 0 ? '↑' : cambio < 0 ? '↓' : '=';

                  return (
                    <span className="cambio-cell" style={{ color: cambioColor }}>
                      {cambioSymbol} {formatCurrency(Math.abs(cambio))}
                    </span>
                  );
                },
              },
            ]}
            data={historial ?? []}
            emptyMessage="No hay registros de histórico"
            keyExtractor={(registro) => registro.historyId}
          />
        </section>

        {/* Gráfico de Línea - Evolución por Sincronización */}
        {historial && historial.length > 0 && (
          <div className="chart-container">
            <h2 className="chart-title">Evolución de Precios</h2>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={[...historial].reverse()}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis
                  dataKey="registeredAt"
                  tickFormatter={(value) => new Date(value).toLocaleDateString('es-ES')}
                />
                <YAxis />
                <Tooltip
                  formatter={(value: number | string | undefined) => formatCurrency(Number(value || 0))}
                  labelFormatter={(label) => new Date(label).toLocaleString('es-ES')}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="costPrice"
                  stroke="#0066cc"
                  name="Precio"
                  dot={{ fill: '#0066cc', r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}

        {/* Insights */}
        <div className="insights-container">
          <h2 className="insights-title">Datos relevantes</h2>
          <div className="insights-list">
            <div className="insight-item">
              <p>
                El precio {variacionReal > 0 ? 'ha aumentado' : variacionReal < 0 ? 'ha disminuido' : 'no ha cambiado'} un{' '}
                {/* Los porcentajes suelen quedarse con toFixed(2) porque no son moneda */}
                <strong>{Math.abs(variacionReal).toFixed(2)}%</strong> desde el registro más antiguo.
              </p>
            </div>

            <div className="insight-item">
              <p>
                El mejor precio registrado es <strong>{formatCurrency(estadisticas.minPrice)}</strong> y el más alto es{' '}
                <strong>{formatCurrency(estadisticas.maxPrice)}</strong>, una diferencia de{' '}
                <strong>{formatCurrency(estadisticas.maxPrice - estadisticas.minPrice)}</strong>.
              </p>
            </div>

            <div className="insight-item">
              <p>
                Precio promedio de <strong>{formatCurrency(estadisticas.averagePrice)}</strong> basado en{' '}
                <strong>{estadisticas.totalRecords}</strong> registros.
              </p>
            </div>

            <div className="insight-item">
              <p>
                Período de análisis desde <strong>{new Date(estadisticas.oldestDate).toLocaleDateString('es-ES')}</strong> hasta{' '}
                <strong>{new Date(estadisticas.mostRecentDate).toLocaleDateString('es-ES')}</strong>.
              </p>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default PriceHistoryPage;