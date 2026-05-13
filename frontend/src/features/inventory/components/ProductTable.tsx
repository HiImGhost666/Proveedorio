import { AggregatedProductDto } from 'src/types';
import { useNavigate } from 'react-router-dom';
import { Badge } from '../../../components/badge/Badge';
import { formatCurrency } from '@/utils/formatting';
import { DataTable, Column } from '@/components/ui/DataTable';
import { SearchX } from 'lucide-react';

export type SortField = 'model' | 'brand' | 'category' | 'precioMinimo' | 'minCostPrice' | 'disponibilidad' | 'availability';
export type SortDirection = 'ASC' | 'DESC';

interface ProductTableProps {
  products: AggregatedProductDto[];
  isLoading: boolean;
  sortBy: SortField;
  direction: SortDirection;
  onSort: (field: SortField) => void;
}

type SpecsRecord = Record<string, unknown>;

const getBadgeVariant = (status: string): 'success' | 'warning' | 'error' | 'neutral' => {
  switch (status) {
    case 'DISPONIBLE': return 'success';
    case 'BAJO_STOCK': return 'warning';
    case 'SIN_STOCK': return 'error';
    default: return 'neutral';
  }
};

const getBadgeText = (status: string): string => {
  switch (status) {
    case 'DISPONIBLE': return 'Disponible';
    case 'BAJO_STOCK': return 'Bajo Stock';
    case 'SIN_STOCK': return 'Agotado';
    default: return status || 'Desconocido';
  }
};

const ProductTable = ({
  products,
  isLoading,
  sortBy,
  direction,
  onSort,
}: ProductTableProps) => {
  const navigate = useNavigate();

  const formatSpecs = (specs: unknown): string => {
    // Si no hay especificaciones, no mostrar nada
    if (!specs) {
      return '';
    }

    let specsObject: unknown;

    // 1. Si es un string, intentar parcearlo como JSON
    if (typeof specs === 'string') {
      try {
        specsObject = JSON.parse(specs);
      } catch {
        // Si no es un JSON válido, podría ser un string simple.
        // Lo mostramos directamente.
        return specs;
      }
    } else {
      specsObject = specs;
    }

    // 2. Si después de parcear, es un objeto (o un array), lo formateamos
    if (typeof specsObject === 'object' && specsObject !== null) {
      // Si el objeto está vacío, no mostrar nada
      if (Object.keys(specsObject).length === 0) {
        return '';
      }
      
      return Object.entries(specsObject as SpecsRecord)
        .map(([key, value]) => {
          // No mostrar "proveedor" y limpiar la key
          if (key.toLowerCase() === 'proveedor') return null;
          const cleanKey = key.replace(/_/g, ' ');
          
          let displayValue: string | number | boolean | null = value as string | number | boolean | null;
          if (typeof value === 'object' && value !== null) {
              displayValue = JSON.stringify(value);
          }

          return `${cleanKey}: ${displayValue}`;
        })
        .filter(Boolean) // Filtrar los nulos (el proveedor)
        .join('\n');
    }

    // 3. Si no es ni string ni objeto, no podemos procesarlo
    return 'No disponible';
  };
  const handleRowClick = (product: AggregatedProductDto) => navigate(`/product/${product.id}`);

  const SortableHeader = ({ field, title, align = 'left' }: { field: SortField; title: string, align?: 'left' | 'center' | 'right' }) => {
    const isActive = sortBy === field;
    const icon = isActive ? (direction === 'ASC' ? '▲' : '▼') : '△';

    const justifyContent = align === 'right' ? 'flex-end' : align === 'center' ? 'center' : 'flex-start';

    return (
      <div className="sortable-header" onClick={() => onSort(field)} style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent, gap: '4px' }}>
        <span>{title}</span>
        <span style={{ fontSize: '0.75rem', opacity: isActive ? 1 : 0.3, color: isActive ? 'var(--brand-500)' : 'inherit' }}>{icon}</span>
      </div>
    );
  };

  const columns: Column<AggregatedProductDto>[] = [
    { header: 'MPN', accessorKey: 'mpn' },
    { header: <SortableHeader field="model" title="Modelo" />, cell: (p) => <span className="font-medium">{p.model}</span> },
    { header: <SortableHeader field="brand" title="Marca" />, accessorKey: 'brand' },
    {
      header: <SortableHeader field="category" title="Categoría" />,
      cell: (p) => p.category || '—',
    },
    { header: <SortableHeader field="minCostPrice" title="Precio Mínimo" align="right" />, cell: (p) => <div className="text-right">{formatCurrency(p.minCostPrice)}</div>, align: 'right' },
    {
      header: <SortableHeader field="availability" title="Disponible" align="center" />,
      cell: (p) => (
        <Badge variant={getBadgeVariant(p.availability)}>
          {getBadgeText(p.availability)}
        </Badge>
      ),
      align: 'center'
    },
    { header: 'Nº Proveedores', cell: (p) => <div className="text-center">{p.supplierCount}</div>, align: 'center' },
    { header: 'Especificaciones', cell: (p) => <div style={{ whiteSpace: 'pre-line' }}>{formatSpecs(p.specifications)}</div> },
  ];

  if (!isLoading && products.length === 0) {
    return (
      <div className="table-message-center">
        <SearchX size={36} style={{ marginBottom: '1rem', opacity: 0.7 }} aria-hidden="true" />
        <p>No hay coincidencias</p>
        <p style={{ fontSize: '0.875rem', opacity: 0.7 }}>Intenta con otros términos de búsqueda</p>
      </div>
    );
  }

  return (
    <DataTable
      columns={columns}
      data={products}
      isLoading={isLoading}
      loadingMessage="Cargando productos..."
      keyExtractor={(p) => p.id}
      onRowClick={handleRowClick}
    />
  );
};

export default ProductTable;