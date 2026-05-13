import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getProductDetails } from 'features/inventory/services/productService';
import { AddCartItem } from 'features/cart/context/CartContext';
import { useCart } from 'features/cart/context/useCart';
import AvailabilityIndicator from 'features/inventory/components/AvailabilityIndicator';
import './ProductDetailPage.css';
import { formatCurrency } from '@/utils/formatting';
import { toast } from 'react-hot-toast';
import { useState, useMemo } from 'react';
import { DataTable } from '@/components/ui/DataTable';
import { SupplierOfferDto } from '@/types';

type OfferSortField = 'supplierName' | 'costPrice' | 'retailPrice' | 'stock' | 'stockStatus' | 'lastSyncAt';

const ProductDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addItem, items } = useCart();
  const [sortField, setSortField] = useState<OfferSortField>('costPrice');
  const [sortDirection, setSortDirection] = useState<'ASC' | 'DESC'>('ASC');

  const SortableColumnHeader = ({ field, title, align = 'left' }: { field: OfferSortField, title: string, align?: 'left' | 'center' | 'right' }) => {
    const isActive = sortField === field;
    const icon = isActive ? (sortDirection === 'ASC' ? '▲' : '▼') : '△';
    return (
      <div
        onClick={() => handleSort(field)}
        style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: align === 'center' ? 'center' : align === 'right' ? 'flex-end' : 'flex-start', gap: '4px', userSelect: 'none' }}
      >
        <span>{title}</span>
        <span style={{ fontSize: '0.8em', opacity: isActive ? 1 : 0.4 }}>{icon}</span>
      </div>
    );
  };

  const { data: product, isLoading, isError } = useQuery({
    queryKey: ['product', id],
    queryFn: () => getProductDetails(Number(id)),
    enabled: !!id,
  });

  const handleSort = (field: OfferSortField) => {
    if (sortField === field) {
      setSortDirection(prev => prev === 'ASC' ? 'DESC' : 'ASC');
    } else {
      setSortField(field);
      setSortDirection('ASC');
    }
  };

  const sortedOfertas = useMemo(() => {
    if (!product?.offers) return [];

    return [...product.offers].sort((a: SupplierOfferDto, b: SupplierOfferDto) => {
      let valA = a[sortField];
      let valB = b[sortField];

      // Handle string vs number comparison
      if (typeof valA === 'string') valA = valA.toLowerCase();
      if (typeof valB === 'string') valB = valB.toLowerCase();

      // Handle missing values (e.g., PVP null)
      if (valA === null || valA === undefined) return sortDirection === 'ASC' ? 1 : -1;
      if (valB === null || valB === undefined) return sortDirection === 'ASC' ? -1 : 1;

      if (valA < valB) return sortDirection === 'ASC' ? -1 : 1;
      if (valA > valB) return sortDirection === 'ASC' ? 1 : -1;
      return 0;
    });
  }, [product?.offers, sortField, sortDirection]);

  const cartQuantitiesBySupplier = useMemo(() => {
    if (!product) {
      return {} as Record<number, number>;
    }

    return items.reduce<Record<number, number>>((acc, item) => {
      if (item.productId === product.id) {
        acc[item.supplierId] = item.quantity;
      }

      return acc;
    }, {});
  }, [items, product]);

  if (isLoading) {
    return <div className="loading-message">Cargando detalles del producto...</div>;
  }

  if (isError || !product) {
    return <div className="error-message">Error al cargar el producto.</div>;
  }

  // Navegar al histórico de precios del proveedor
  const handleViewPriceHistory = (proveedorId: number) => {
    navigate(`/historial-precios/${id}/${proveedorId}`);
  };


  const handleAddToCart = (oferta: SupplierOfferDto) => {
    if (!product) return;

    if (oferta.stock <= 0) {
      toast.error('No hay stock disponible para esta oferta.');
      return;
    }

    const itemToAdd: AddCartItem = {
      productId: product.id,
      supplierId: oferta.supplierId,
      productName: `${product.brand} ${product.model}`,
      supplierName: oferta.supplierName,
      unitPrice: oferta.costPrice ?? 0,
      quantity: 1,
      stock: oferta.stock,
      mpn: product.mpn,
      pvpPrice: oferta.retailPrice ?? undefined,
      // unit, productUrl, supplierRef, notes can be filled later if needed
    };

    const result = addItem(itemToAdd);

    if (result.status === 'max-stock-reached') {
      toast.error(`No puedes añadir más unidades. Stock disponible: ${result.maxQuantity}.`);
      return;
    }

    if (result.quantity >= result.maxQuantity) {
      toast.success(`'${product.model}' añadido. Has alcanzado el stock disponible.`);
      return;
    }

    toast.success(`'${product.model}' añadido a la cesta.`);
  };

  return (
    <div className="product-detail-container">
      <div className="product-detail-card">
        <div className="mb-6">
          <Link to="/" className="back-link">
            &larr; Volver a la lista
          </Link>
        </div>

        <div className="product-header">
          <h1 className="product-title">{product.model}</h1>
          <p className="product-subtitle">{product.brand} - <span className="product-mpn">{product.mpn}</span></p>

          <div className="specifications-grid">
            <div>
              <h2 className="specifications-title">Especificaciones</h2>
              <div className="specifications-list">
                {Object.entries(product.specifications || {}).map(([key, value]) => (
                  <div key={key} className="specification-item">
                    <p className="specification-key">{key.replace(/_/g, ' ')}</p>
                    <p className="specification-value">{value}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div>
          <h2 className="offers-title">Ofertas de Proveedores</h2>
          <div className="offers-table-container">
            <DataTable
              columns={[
                {
                  header: <SortableColumnHeader field="supplierName" title="Proveedor" />,
                  accessorKey: 'supplierName',
                  cell: (item) => <span className="font-medium">{item.supplierName}</span>
                },
                {
                  header: <SortableColumnHeader field="costPrice" title="Precio Partner" align="right" />,
                  accessorKey: 'costPrice',
                  align: 'right',
                  cell: (item) => <div style={{ fontWeight: 600, color: 'var(--brand-600)', textAlign: 'right' }}>{formatCurrency(item.costPrice)}</div>
                },
                {
                  header: <SortableColumnHeader field="retailPrice" title="PVP" align="right" />,
                  accessorKey: 'retailPrice',
                  align: 'right',
                  cell: (item) => <div style={{ textAlign: 'right' }}>{item.retailPrice != null ? formatCurrency(item.retailPrice) : 'N/A'}</div>
                },
                {
                  header: <SortableColumnHeader field="stock" title="Stock" align="center" />,
                  accessorKey: 'stock',
                  align: 'center',
                  cell: (item) => <div style={{ textAlign: 'center' }}>{item.stock}</div>
                },
                {
                  header: <SortableColumnHeader field="stockStatus" title="Estado" align="center" />,
                  accessorKey: 'stockStatus',
                  align: 'center',
                  cell: (item) => <div style={{ display: 'flex', justifyContent: 'center' }}><AvailabilityIndicator status={item.stockStatus} /></div>
                },
                {
                  header: <SortableColumnHeader field="lastSyncAt" title="Última Sincronización" />,
                  accessorKey: 'lastSyncAt',
                  cell: (item) => new Date(item.lastSyncAt).toLocaleString()
                },
                {
                  header: 'Acciones',
                  align: 'center',
                  cell: (item) => (
                    (() => {
                      const currentQuantity = cartQuantitiesBySupplier[item.supplierId] ?? 0;
                      const isCartLimitReached = item.stock > 0 && currentQuantity >= item.stock;

                      return (
                        <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                          <button
                            className="btn-action btn-action-edit"
                            onClick={(e) => { e.stopPropagation(); handleViewPriceHistory(item.supplierId); }}
                            title="Ver histórico de precios"
                          >
                            Histórico
                          </button>
                          <button
                            className="btn-primary"
                            style={{ padding: '0.6rem', display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                            onClick={(e) => { e.stopPropagation(); handleAddToCart(item); }}
                            title={isCartLimitReached ? 'Stock máximo ya añadido al carrito' : 'Añadir a cesta'}
                            disabled={item.stock <= 0 || isCartLimitReached}
                          >
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <circle cx="9" cy="21" r="1"></circle>
                              <circle cx="20" cy="21" r="1"></circle>
                              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                            </svg>
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                              <line x1="12" y1="5" x2="12" y2="19"></line>
                              <line x1="5" y1="12" x2="19" y2="12"></line>
                            </svg>
                          </button>
                        </div>
                      );
                    })()
                  )
                }
              ]}
              data={sortedOfertas}
              keyExtractor={(item) => item.supplierId?.toString() || item.supplierName}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductDetailPage;