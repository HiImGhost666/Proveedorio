import { useState, useMemo, type ChangeEvent, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getProducts, searchProducts } from 'features/inventory/services/productService';
import ProductTable, { SortField, SortDirection } from 'features/inventory/components/ProductTable';
import Pagination from 'features/inventory/components/Pagination';
import { useDebounce } from '../../../hooks/useDebounce';
import './InventoryDashboardPage.css';
import ErrorBoundary from '@/components/ui/ErrorBoundary';
import { Search, SlidersHorizontal } from 'lucide-react';

const InventoryDashboardPage = () => {
  const [queryInput, setQueryInput] = useState('');
  const [specsInput, setSpecsInput] = useState('');
  const [query, setQuery] = useState('');
  const [specsFilter, setSpecsFilter] = useState('');

  const debouncedSpecsFilter = useDebounce(specsFilter, 300);
  const [page, setPage] = useState(0);
  const [sortBy, setSortBy] = useState<SortField>('model');
  const [direction, setDirection] = useState<SortDirection>('ASC');
  const PAGE_SIZE = 20;

  // Fetch products via API based on query, sort, page AND specsFilter
  const { data: productsResponse, isLoading, isFetching, isError, refetch } = useQuery({
    queryKey: ['products', query, page, sortBy, direction, debouncedSpecsFilter],
    queryFn: () => {
      // If either the main query OR the specs filter has text, we MUST use the /search endpoint.
      if (query.trim() === '' && debouncedSpecsFilter.trim() === '') {
        return getProducts(page, PAGE_SIZE, sortBy, direction);
      } else {
        return searchProducts(query, page, PAGE_SIZE, sortBy, direction, undefined, debouncedSpecsFilter);
      }
    },
    refetchInterval: 6 * 60 * 60 * 1000,
  });

  const productsFromApi = useMemo(() => productsResponse?.content ?? [], [productsResponse?.content]);
  const totalPages = productsResponse?.totalPages ?? 0;

  // Handlers for search locally in this page
  const handleSearchSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setQuery(queryInput);
    setPage(0);
  };

  const handleSpecsChange = (e: ChangeEvent<HTMLInputElement>) => {
    setSpecsInput(e.target.value);
    setSpecsFilter(e.target.value);
    setPage(0);
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);

    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handleSort = (newSortBy: SortField) => {
    if (sortBy === newSortBy) {
      setDirection(direction === 'ASC' ? 'DESC' : 'ASC');
    } else {
      setSortBy(newSortBy);
      setDirection('ASC');
    }
    setPage(0);
  };

  const isDataLoading = isLoading || isFetching;
  const totalElements = productsResponse?.totalElements ?? 0;

  return (
    <div className="list-page-container dashboard-container">
      <div className="page-header" style={{ marginBottom: "1rem" }}>
        <div>
          <h1 className="page-title">Inventario Consolidado</h1>
          <p className="page-subtitle">
            {totalElements === 0 ? 'Sin resultados disponibles' : `${totalElements} productos localizados`}
          </p>
        </div>
      </div>

      <div className="dashboard-table-section" style={{ marginBottom: "1.5rem" }}>
        <div className="table-search-container">
          <form className="search-inputs-row" onSubmit={handleSearchSubmit}>
            <div className="search-input-wrapper primary-search">
              <Search size={16} className="search-input-icon" aria-hidden="true" />
              <input
                type="text"
                placeholder="Buscar por Modelo, Marca, MPN..."
                className="search-input"
                value={queryInput}
                onChange={(e) => setQueryInput(e.target.value)}
              />
            </div>
            <div className="search-input-wrapper secondary-search">
              <SlidersHorizontal size={16} className="search-input-icon" aria-hidden="true" />
              <input
                type="text"
                placeholder="Filtrar Specs (ej: 'ram:8gb' o 'rojo')"
                className="search-input"
                value={specsInput}
                onChange={handleSpecsChange}
              />
            </div>
            <button type="submit" className="search-button">
              <Search size={16} aria-hidden="true" />
              Buscar
            </button>
          </form>
        </div>

        <ErrorBoundary fallbackMessage="Error al cargar los productos." onRetry={refetch}>
          {isError ? (
            <div className="table-message-center" style={{ color: 'var(--color-danger)' }}>Error al conectar con el servidor.</div>
          ) : (
            <ProductTable
              products={productsFromApi}
              isLoading={isDataLoading}
              sortBy={sortBy}
              direction={direction}
              onSort={handleSort}
            />
          )}
        </ErrorBoundary>
      </div>

      {totalPages > 1 && (
        <Pagination
          page={page}
          totalPages={totalPages}
          onPageChange={handlePageChange}
        />
      )}
    </div>
  );
};

export default InventoryDashboardPage;
