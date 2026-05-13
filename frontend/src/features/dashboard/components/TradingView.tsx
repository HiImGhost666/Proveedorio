import React, { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getProducts } from 'features/inventory/services/productService';
import { formatCurrency } from '@/utils/formatting';
import { useNavigate } from 'react-router-dom';
import { DataTable, Column } from '../../../components/ui/DataTable';
import { Badge } from '../../../components/badge/Badge';
import type { AggregatedProductDto } from 'src/types';

type TradingOpportunity = AggregatedProductDto & { gap: number; marginPct: number };
type SortDirection = 'asc' | 'desc';
type SortKey = 'mpn' | 'model' | 'minCostPrice' | 'minRetailPrice' | 'gap' | 'marginPct';

const TradingView: React.FC = () => {
    // Fetch products (ideally this would be a specific endpoint for top opportunities,
    // but for now we fetch recent products and calculate the gap)
    const { data, isLoading, isError } = useQuery({
        queryKey: ['tradingProducts'],
        queryFn: () => getProducts(0, 50, 'disponibilidad', 'DESC'),
    });

    const opportunities = useMemo(() => {
        const products = data?.content ?? [];
        return products
            .filter((p) => p.minRetailPrice && p.minCostPrice && p.minRetailPrice > p.minCostPrice)
            .map<TradingOpportunity>((p) => {
                const gap = (p.minRetailPrice || 0) - (p.minCostPrice || 0);
                const marginPct = p.minCostPrice ? (gap / p.minCostPrice) * 100 : 0;
                return {
                    ...p,
                    gap,
                    marginPct,
                };
            })
            .sort((a, b) => b.gap - a.gap); // Sort by highest gap first
    }, [data?.content]);

    const maxDiscountItem = opportunities.length > 0 ? opportunities[0] : null;

    const navigate = useNavigate();
    const [sortConfig, setSortConfig] = useState<{ key: SortKey, direction: SortDirection } | null>(null);

    const compareValues = (a: TradingOpportunity, b: TradingOpportunity, key: SortKey): number => {
        switch (key) {
            case 'mpn':
            case 'model':
                return (a[key] ?? '').localeCompare(b[key] ?? '');
            case 'minCostPrice':
            case 'minRetailPrice':
            case 'gap':
            case 'marginPct':
                return (a[key] ?? 0) - (b[key] ?? 0);
            default:
                return 0;
        }
    };

    const sortedOpportunities = useMemo(() => {
        const sortableItems = [...opportunities];
        if (sortConfig !== null) {
            sortableItems.sort((a, b) => {
                const baseResult = compareValues(a, b, sortConfig.key);
                return sortConfig.direction === 'asc' ? baseResult : -baseResult;
            });
        }
        return sortableItems;
    }, [opportunities, sortConfig]);

    const handleSort = (key: SortKey) => {
        let direction: SortDirection = 'asc';
        if (sortConfig && sortConfig.key === key && sortConfig.direction === 'asc') {
            direction = 'desc';
        }
        setSortConfig({ key, direction });
    };

    const renderSortableHeader = (title: string, key: SortKey, align: 'left' | 'right' | 'center' = 'left') => {
        const isActive = sortConfig?.key === key;
        let icon = '△';
        if (isActive) {
            icon = sortConfig.direction === 'asc' ? '▲' : '▼';
        }

        let justifyContent = 'flex-start';
        if (align === 'right') justifyContent = 'flex-end';
        if (align === 'center') justifyContent = 'center';

        return (
            <div onClick={() => handleSort(key)} style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', justifyContent }}>
                <span>{title}</span>
                <span style={{ fontSize: '0.75rem', opacity: isActive ? 1 : 0.3, color: isActive ? 'var(--brand-500)' : 'inherit' }}>{icon}</span>
            </div>
        );
    };

    const columns: Column<TradingOpportunity>[] = [
        {
            header: renderSortableHeader('MPN', 'mpn'),
            cell: (item) => <div style={{ color: 'var(--text-primary)' }}>{item.mpn}</div>,
        },
        {
            header: renderSortableHeader('Producto', 'model'),
            cell: (item) => (
                <div>
                    <div className="font-medium" style={{ color: 'var(--text-primary)' }}>{item.model}</div>
                    <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{item.brand}</div>
                </div>
            ),
        },
        {
            header: renderSortableHeader('Mejor Precio', 'minCostPrice', 'right'),
            align: 'right',
            cell: (item) => <div style={{ textAlign: 'right', fontWeight: '500', color: 'var(--text-primary)' }}>{formatCurrency(item.minCostPrice)}</div>,
        },
        {
            header: renderSortableHeader('PVP Mínimo', 'minRetailPrice', 'right'),
            align: 'right',
            cell: (item) => (
                <div style={{ textAlign: 'right', color: 'var(--text-secondary)' }}>
                    {item.minRetailPrice ? formatCurrency(item.minRetailPrice) : 'N/A'}
                </div>
            ),
        },
        {
            header: renderSortableHeader('Brecha Bruta', 'gap', 'right'),
            align: 'right',
            cell: (item) => <div style={{ textAlign: 'right', fontWeight: 'bold', color: 'var(--brand-600)' }}>+{formatCurrency(item.gap)}</div>,
        },
        {
            header: renderSortableHeader('Margen %', 'marginPct', 'right'),
            align: 'right',
            cell: (item) => {
                const isHighMargin = item.marginPct > 20;
                return (
                    <div style={{ textAlign: 'right' }}>
                        <Badge variant={isHighMargin ? 'success' : 'info'}>
                            +{item.marginPct.toFixed(1)}%
                        </Badge>
                    </div>
                );
            },
        },
        {
            header: 'Acciones',
            align: 'center',
            cell: (item) => (
                <div className="actions-cell" style={{ justifyContent: 'center' }}>
                    <button
                        onClick={(e) => { e.stopPropagation(); navigate(`/product/${item.id}`); }}
                        className="btn-action btn-action-edit"
                    >
                        Ver Detalle
                    </button>
                </div>
            )
        }
    ];

    if (isLoading) {
        return (
            <div style={{ padding: '2rem' }}>
                <div style={{ animation: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div style={{ backgroundColor: 'var(--bg-tertiary)', height: '6rem', borderRadius: 'var(--radius-lg)', width: '100%' }}></div>
                    <div style={{ backgroundColor: 'var(--bg-tertiary)', height: '16rem', borderRadius: 'var(--radius-lg)', width: '100%' }}></div>
                </div>
            </div>
        );
    }

    if (isError) {
        return <div style={{ color: 'var(--color-danger)', fontWeight: '500' }}>Error al cargar datos de oportunidades.</div>;
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {/* Hero KPIs */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem' }}>
                <div className="dashboard-card">
                    <h3 style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', margin: 0 }}>Mayor Descuento/Margen</h3>
                    <p style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--text-primary)', marginTop: '0.5rem', marginBottom: '0.25rem' }}>
                        {maxDiscountItem ? `+${maxDiscountItem.marginPct.toFixed(1)}%` : 'N/A'}
                    </p>
                    {maxDiscountItem && (
                        <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={maxDiscountItem.model}>
                            {maxDiscountItem.brand} - {maxDiscountItem.model}
                        </p>
                    )}
                </div>
                <div className="dashboard-card">
                    <h3 style={{ fontSize: '0.875rem', fontWeight: '500', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', margin: 0 }}>Oportunidades Detectadas</h3>
                    <p style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--text-primary)', marginTop: '0.5rem', marginBottom: '0.25rem' }}>{opportunities.length}</p>
                    <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0 }}>Con margen PVP positivo</p>
                </div>
            </div>

            {/* Radar de Oportunidades */}
            <div style={{ marginBottom: '0.5rem', marginTop: '1rem' }}>
                <h2 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', margin: '0 0 0.25rem 0' }}>Radar de Ofertas Inmediatas</h2>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0 }}>Productos ordenados por mayor brecha nominal entre el Mejor Coste y el PVP Recomendado Mínimo.</p>
            </div>
            <DataTable
                columns={columns}
                data={sortedOpportunities}
                keyExtractor={(item) => item.id}
                onRowClick={(item) => navigate(`/product/${item.id}`)}
                emptyMessage="No se encontraron oportunidades con margen positivo en los últimos productos."
            />
        </div>
    );
};

export default TradingView;
