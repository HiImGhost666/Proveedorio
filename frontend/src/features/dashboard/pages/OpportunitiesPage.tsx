import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ArrowUp, ArrowDown } from 'lucide-react';

import { TopProductDto, TopMoverDto, StaleProductDto } from 'src/types';
import { getDashboardSummary } from '../services/dashboardService';
import TradingView from '../components/TradingView';
import { formatCurrency } from '@/utils/formatting';
import { DataTable } from '@/components/ui/DataTable';
import './DashboardPage.css'; // Reuse styles for now

// --- Reusable Components ---

const TableCard = ({ title, children, subTitle }: { title: string, children: React.ReactNode, subTitle?: string }) => (
  <div className={`dashboard-card ${subTitle ? 'zombies-card' : ''}`}>
    <h3 className="card-title">{title}</h3>
    {subTitle && <p className="text-sm text-gray-500" style={{ marginTop: "-1rem", marginBottom: "1rem" }}>{subTitle}</p>}
    {children}
  </div>
);

// --- Child Components ---

const TopProductsTable = ({ data }: { data: TopProductDto[] }) => (
  <DataTable
    columns={[
      {
        header: 'Producto',
        cell: (product) => <div className="table-product-name">{product.name}</div>,
      },
      {
        header: 'Proveedor',
        accessorKey: 'supplierName',
      },
      {
        header: 'Precio',
        cell: (product) => formatCurrency(product.price),
        align: 'right',
      },
      {
        header: 'Acciones',
        align: 'center',
        cell: (product) =>
          product.productId ? (
            <Link to={`/product/${product.productId}`} title="Ver ficha de producto">
              <button className="btn-action btn-action-edit">Ver Detalle</button>
            </Link>
          ) : null,
      },
    ]}
    data={data}
    emptyMessage="No hay productos para mostrar."
    keyExtractor={(product) => product.productId ?? product.name}
  />
);

const TopMoversTable = ({ data, type }: { data: TopMoverDto[], type: 'up' | 'down' }) => {
  const isUp = type === 'up';
  const colorClass = isUp ? 'price-up' : 'price-down';
  const Icon = isUp ? ArrowUp : ArrowDown;

  return (
    <DataTable
      columns={[
        {
          header: 'Producto',
          cell: (item) => <div className="table-product-name">{item.productName}</div>,
        },
        {
          header: 'Proveedor',
          accessorKey: 'supplierName',
        },
        {
          header: 'Precio Actual',
          cell: (item) => formatCurrency(item.currentPrice),
          align: 'right',
        },
        {
          header: 'Variación',
          align: 'center',
          cell: (item) => (
            <span className={`price-variation ${colorClass}`}>
              <Icon />
              {item.percentChange.toFixed(2)}%
            </span>
          ),
        },
        {
          header: 'Acciones',
          align: 'center',
          cell: (item) =>
            item.productId ? (
              <Link to={`/product/${item.productId}`} title="Ver ficha de producto">
                <button className="btn-action btn-action-edit">Ver Detalle</button>
              </Link>
            ) : null,
        },
      ]}
      data={data}
      emptyMessage="No hay datos de movimiento de precios para mostrar."
      keyExtractor={(item) => item.productId ?? `${item.productName}-${item.supplierName}`}
    />
  );
};

const ZombiesTable = ({ products }: { products: StaleProductDto[] }) => {
  if (!products || products.length === 0) return null;

  return (
    <DataTable
      columns={[
        {
          header: 'MPN',
          cell: (p) => <span className="font-mono text-xs">{p.mpn}</span>,
        },
        {
          header: 'Producto',
          cell: (p) => <span className="table-product-name">{p.name}</span>,
        },
        {
          header: 'Última Act.',
          cell: (p) => new Date(p.lastUpdatedAt).toLocaleDateString(),
        },
        {
          header: 'Días Inactivo',
          cell: (p) => (
            <span className="inactive-days-badge">
              {p.daysWithoutUpdate} días
            </span>
          ),
          align: 'center',
        },
        {
          header: 'Acciones',
          align: 'center',
          cell: (p) =>
            p.id ? (
              <Link to={`/product/${p.id}`} title="Ver ficha de producto">
                <button className="btn-action btn-action-edit">Ver Detalle</button>
              </Link>
            ) : null,
        },
      ]}
      data={products}
      emptyMessage="No hay productos zombies para mostrar."
      keyExtractor={(p) => p.id}
    />
  );
};


// --- Main Opportunities Page Component ---

const OpportunitiesPage = () => {
  const { data: summary, isLoading, error } = useQuery({
    queryKey: ['dashboardSummary'],
    queryFn: getDashboardSummary,
  });

  const isDataReady = !isLoading && !error && summary;

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1 className="dashboard-title">Oportunidades y Alertas</h1>
      </header>
      <main style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
        <TradingView />
        {isDataReady && (
          <>
            <TableCard title="Alertas: Subidas de Precio">
              <TopMoversTable data={summary.topPriceIncreases} type="up" />
            </TableCard>

            <TableCard title="Oportunidades: Bajadas de Precio">
              <TopMoversTable data={summary.topPriceDecreases} type="down" />
            </TableCard>

            <TableCard title="Top 5 Productos Más Caros">
              <TopProductsTable data={summary.topExpensiveProducts} />
            </TableCard>

            {summary.staleProducts && summary.staleProducts.length > 0 && (
              <div>
                <TableCard
                  title='Productos "Zombies"'
                  subTitle="Productos sin actualización en +30 días. Revisa la conexión con el proveedor."
                >
                  <ZombiesTable products={summary.staleProducts} />
                </TableCard>
              </div>
            )}
          </>
        )}
        {!isDataReady && (
          <div className="centered-message">Cargando datos de oportunidades...</div>
        )}
      </main>
    </div>
  );
};

export default OpportunitiesPage;
