import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import './SupplierDashboardPage.css';
import api from 'services/api';
import { AggregatedProductDto, SearchResult, SupplierDashboardDto } from 'src/types';
import ProductTable, { SortField, SortDirection } from 'features/inventory/components/ProductTable';

// --- Función para obtener productos del proveedor (usando el mismo DTO que el inventario consolidado) ---
const getSupplierProducts = async (
  id: number,
  sortBy: SortField,
  direction: SortDirection,
): Promise<AggregatedProductDto[]> => {
  const effectiveSortBy = (sortBy === 'disponibilidad' || sortBy === 'availability') ? 'disponibilidadRank' : sortBy;

  const response = await api.get(`/productos/search`, {
    params: {
      proveedorId: id,
      page: 0,
      size: 20,
      sortBy: effectiveSortBy,
      direction,
    },
  });

  return (response.data.data as SearchResult).content;
};

const SupplierDashboardPage = () => {
  const { id } = useParams<{ id: string }>();
  const [sortBy, setSortBy] = useState<SortField>('model');
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC');

  // --- Datos del dashboard (Endpoint permitido para CLIENTE) ---
  const { data: dashboardData, isLoading, isError } = useQuery<SupplierDashboardDto>({
    queryKey: ['supplierDashboard', id],
    queryFn: async () => {
      const response = await api.get(`/proveedores/${id}/dashboard`);
      return response.data.data;
    },
    enabled: !!id,
  });

  // --- Productos del proveedor (mismo formato que el inventario consolidado) ---
  const { data: products, isLoading: isLoadingProducts } = useQuery<AggregatedProductDto[]>({
    queryKey: ['supplierProducts', id, sortBy, sortDirection],
    queryFn: () => getSupplierProducts(Number(id), sortBy, sortDirection),
    enabled: !!id,
  });

  const handleSort = (column: SortField) => {
    if (sortBy === column) {
      setSortDirection(prev => (prev === 'ASC' ? 'DESC' : 'ASC'));
    } else {
      setSortBy(column);
      setSortDirection('ASC');
    }
  };

  if (isLoading) {
    return <div className="p-8 text-center text-white">Cargando detalles del proveedor...</div>;
  }

  if (isError) {
    return <div className="p-8 text-center text-red-500">Error al cargar los detalles del proveedor.</div>;
  }

  return (
    <div className="supplier-dashboard-page">
      <div className="detail-container">
        <div className="detail-header">
          <h1 className="detail-title">{dashboardData?.supplierName}</h1>
          <Link to="/suppliers" className="back-link">
            ← Volver al listado
          </Link>
        </div>

        <div className="products-view">
          {/* KPIs */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
              <p className="text-sm text-gray-500 mb-1">Productos vinculados</p>
              <p className="text-2xl font-bold text-gray-800">{dashboardData?.totalProducts || 0}</p>
            </div>
            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
              <p className="text-sm text-gray-500 mb-1">Fuera de stock</p>
              <p className="text-2xl font-bold text-red-600">{dashboardData?.productsOutOfStock || 0}</p>
            </div>
            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
              <p className="text-sm text-gray-500 mb-1">Latencia media</p>
              <p className="text-2xl font-bold text-blue-600">
                {dashboardData?.avgSyncLatencyHours ? `${(dashboardData.avgSyncLatencyHours * 60 * 60 * 1000).toFixed(0)}ms` : 'N/A'}
              </p>
            </div>
          </div>

          {/* Products Table */}
          <div className="table-container mt-6">
            {isLoadingProducts ? (
              <div className="loading-message">Cargando productos...</div>
            ) : products && products.length > 0 ? (
              <ProductTable
                products={products}
                isLoading={false}
                sortBy={sortBy}
                direction={sortDirection}
                onSort={handleSort}
              />
            ) : (
              <div className="no-products-message">No se encontraron productos para este proveedor.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SupplierDashboardPage;