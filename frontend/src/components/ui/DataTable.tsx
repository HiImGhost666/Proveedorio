import React from 'react';

export interface Column<T> {
    header: React.ReactNode;
    accessorKey?: keyof T;
    cell?: (item: T, index: number) => React.ReactNode;
    align?: 'left' | 'center' | 'right';
}

interface DataTableProps<T> {
    columns: Column<T>[];
    data: T[];
    isLoading?: boolean;
    isError?: boolean;
    loadingMessage?: string;
    errorMessage?: string;
    emptyMessage?: string;
    onRowClick?: (item: T) => void;
    keyExtractor: (item: T, index: number) => string | number;
}

/**
 * Componente presentacional (Dummy) genérico para renderizar tablas
 * usando los estilos globales de index.css
 */
export function DataTable<T>({
    columns,
    data,
    isLoading = false,
    isError = false,
    loadingMessage = 'Cargando datos...',
    errorMessage = 'Error al cargar los datos.',
    emptyMessage = 'No hay datos disponibles.',
    onRowClick,
    keyExtractor,
}: DataTableProps<T>) {

    if (isLoading) {
        return <div className="table-message-center">{loadingMessage}</div>;
    }

    if (isError) {
        return <div className="table-message-center" style={{ color: 'var(--color-danger)' }}>{errorMessage}</div>;
    }

    if (!data || data.length === 0) {
        return <div className="table-message-center">{emptyMessage}</div>;
    }

    return (
        <div className="table-container">
            <table className="data-table">
                <thead>
                    <tr>
                        {columns.map((col, index) => (
                            <th key={index} style={{ textAlign: col.align || 'left' }}>{col.header}</th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {data.map((item, rowIndex) => (
                        <tr
                            key={keyExtractor(item, rowIndex)}
                            className={onRowClick ? "clickable-row" : ""}
                            onClick={() => onRowClick && onRowClick(item)}
                        >
                            {columns.map((col, idx) => {
                                let content: React.ReactNode = null;

                                if (col.cell) {
                                    // Renderizado customizado de la celda
                                    content = col.cell(item, rowIndex);
                                } else if (col.accessorKey) {
                                    // Acceso directo a la propiedad
                                    const val = item[col.accessorKey];
                                    content = val as React.ReactNode;
                                }

                                return <td key={idx} style={{ textAlign: col.align || 'left' }}>{content}</td>;
                            })}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
