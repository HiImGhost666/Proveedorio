import { useQuery } from '@tanstack/react-query';
import { getSuppliers } from '../services/supplierService';
import { Link } from 'react-router-dom';
import { SupplierSimpleDto } from 'src/types';
import { useAuth } from 'features/auth/context/AuthContext';
import { DataTable, Column } from '@/components/ui/DataTable';
import ErrorBoundary from '@/components/ui/ErrorBoundary';

const SupplierListPage = () => {
  const { isAdmin } = useAuth();

  const { data: suppliers, isLoading, isError, refetch } = useQuery<SupplierSimpleDto[]>({
    queryKey: ['suppliers'],
    queryFn: getSuppliers,
  });

  const columns: Column<SupplierSimpleDto>[] = [
    { header: 'Nombre', accessorKey: 'name' },
    {
      header: 'Acciones',
      align: 'right',
      cell: (supplier) => (
        <div className="actions-cell">
          {isAdmin && (
            <Link to={`/suppliers/${supplier.id}`} className="btn-action btn-action-edit">
              Detalles
            </Link>
          )}
          <Link to={`/suppliers/${supplier.id}/dashboard`} className="btn-action btn-action-edit">
            Dashboard
          </Link>
        </div>
      ),
    },
  ];

  return (
    <div className="list-page-container">
      <div className="page-header">
        <h1 className="page-title">Gestión de proveedores</h1>
        {isAdmin && (
          <Link to="/suppliers/new" className="btn-create">
            Crear proveedor
          </Link>
        )}
      </div>

      <ErrorBoundary
        fallbackMessage="Error al cargar la lista de proveedores. Verifique la conexión con el servidor."
        onRetry={refetch}
      >
          <DataTable
            columns={columns}
            data={suppliers || []}
            isLoading={isLoading}
            isError={isError}
            loadingMessage="Cargando proveedores..."
            keyExtractor={(supplier) => supplier.id}
          />
      </ErrorBoundary>
    </div>
  );
};

export default SupplierListPage;